package nl.vu.zorilla.primaryCopy;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortIdentifier;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import java.util.Random;
import java.util.UUID;

import org.apache.log4j.Logger;

import nl.vu.zorilla.Config;
import nl.vu.zorilla.Node;
import nl.vu.zorilla.ZorillaException;
import nl.vu.zorilla.io.Hash;
import nl.vu.zorilla.io.ObjectInput;
import nl.vu.zorilla.io.ObjectOutput;
import nl.vu.zorilla.jobNet.Call;
import nl.vu.zorilla.jobNet.EndPoint;
import nl.vu.zorilla.jobNet.Invocation;
import nl.vu.zorilla.jobNet.Receiver;

public class InputFile implements nl.vu.zorilla.io.InputFile, Receiver {

    private static final int REQUEST_BLOCKS = 0;

    private static final Logger logger = Logger.getLogger(InputFile.class);

    private static int blockPosition(long position) {
        return (int) (position / Config.FILE_BLOCK_SIZE);
    }

    // returns the index of the first block AFTER or ON this position
    private static int blockLimit(long limit) {
        int result = (int) (limit / Config.FILE_BLOCK_SIZE);
        if ((limit % Config.FILE_BLOCK_SIZE) > 0) {
            result += 1;
        }
        return result;
    }

    private static long position(int blockNr) {
        return blockNr * Config.FILE_BLOCK_SIZE;
    }

    private static long limit(int blockNr) {
        return position(blockNr) + Config.FILE_BLOCK_SIZE;
    }

    private static FileChannel createFileChannel(java.io.File file)
            throws IOException {

        return new java.io.RandomAccessFile(file, "rw").getChannel();
    }

    private final String path;

    private final File file;

    private final UUID id;

    private final EndPoint endPoint;

    private final Job job;

    private final long size;

    private final Hash[] hashes;

    private final ReceivePortIdentifier primary;

    // available blocks, Config.FILE_BLOCK_SIZE chunks
    private BitSet availableBlocks;

    private Random random = new Random();

    // create a primary input file
    public InputFile(String virtualPath, File file, Primary p)
            throws ZorillaException, IOException {
        this.path = virtualPath;
        this.id = Node.generateUUID();
        this.job = p;
        this.file = file;

        if (virtualPath != null
                && !(virtualPath.startsWith("/") || virtualPath
                        .equals("<stdin>"))) {
            throw new ZorillaException("virtual path (" + virtualPath
                    + ") must be absolute");
        }

        if (!file.isAbsolute()) {
            throw new ZorillaException("File (" + file + ") not absolute");
        }

        if (file.isDirectory()) {
            throw new ZorillaException("File (" + file + ") is a directory");
        }

        if (!file.canRead()) {
            throw new ZorillaException("cannot read from file (" + file + ")");
        }

        logger.debug("new input file: " + file + " for " + virtualPath);

        FileChannel channel = createFileChannel(file);
        size = channel.size();

        // create hashes
        int nrOfBlocks = blockLimit(size);
        hashes = new Hash[nrOfBlocks];

        for (int i = 0; i < nrOfBlocks; i++) {
            long position = position(i);
            long limit = limit(i);
            if (limit > size) {
                limit = size;
            }
            hashes[i] = new Hash(channel, position, limit);
        }
        channel.close();

        availableBlocks = new BitSet();
        availableBlocks.set(0, nrOfBlocks);

        endPoint = job.newEndPoint(id.toString(), this);

        primary = endPoint.getID();
    }

    // create a copy input file
    public InputFile(ObjectInput in, Copy copy) throws IOException,
            ZorillaException {

        this.job = copy;

        try {

            path = in.readString();
            id = (UUID) in.readObject();
            size = in.readLong();
            primary = (ReceivePortIdentifier) in.readObject();
            hashes = (Hash[]) in.readObject();

        } catch (ClassNotFoundException e) {
            throw new ZorillaException("could not read bootstrap", e);
        }

        // nothing available, create empty list
        availableBlocks = new BitSet();

        file = File.createTempFile("zorilla", ".tmp");

        endPoint = job.newEndPoint(id.toString(), this);

    }

    public long size() {
        return size;
    }

    public void writeBootStrap(ObjectOutput output) throws IOException {
        output.writeString(path());
        output.writeObject(id());
        output.writeLong(size);
        output.writeObject(primary);
        output.writeObject(hashes);
    }

    // create a copy input file

    public String path() {
        return path;
    }

    public UUID id() {
        return id;
    }

    public int read(long fileOffset, byte[] data, int offset, int length)
            throws ZorillaException {
        try {
            if (fileOffset >= size()) {
                return -1;
            } else if ((fileOffset + length) > size()) {
                length = (int) (size() - fileOffset);
            }

            FileChannel channel = createFileChannel(file);

            download(fileOffset, length, channel);

            channel.position(fileOffset);
            ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);

            while (buffer.hasRemaining()) {
                channel.read(buffer);
            }
            channel.close();

        } catch (IOException e) {
            throw new ZorillaException("error reading from file");
        }

        return length;
    }

    public File copyTo(File dir) throws ZorillaException {

        logger.debug("copying " + file + " to " + dir);

        if (path == null) {
            throw new ZorillaException(
                    "cannot copy input file without virtual path");
        }

        File destFile = new File(dir.getAbsolutePath() + path);
        destFile.getParentFile().mkdirs();

        if (!destFile.getParentFile().isDirectory()) {
            throw new ZorillaException("could not create directory: "
                    + destFile.getParentFile());
        }

        logger.debug("destination file = " + destFile);

        try {

            FileChannel source = createFileChannel(file);

            // make sure the file is complete
            download(0, size(), source);

            source.position(0);

            FileChannel destination = createFileChannel(destFile);
            destination.position(0);

            long remaining = source.size();
            long offset = 0;

            while (remaining > 0) {
                long count = source.transferTo(offset, remaining, destination);
                remaining -= count;
                offset += count;
            }

            source.close();
            destination.close();

        } catch (IOException e) {
            throw new ZorillaException("could not copy file", e);
        }

        return destFile;
    }

    public void receive(ReadMessage message) {
        job.log("message received in file", new ZorillaException(
                "message received in file"));
    }

    private void readFromMessage(ObjectInput in, FileChannel channel,
            long position, long limit) throws IOException {
        byte[] bytes = new byte[32 * 1000];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        if (limit > size()) {
            throw new IOException("cannot write past end of file");
        }

        while (position < limit) {
            int read = (int) Math.min((limit - position), bytes.length);

            in.readArray(bytes, 0, read);
            buffer.position(0);
            buffer.limit(read);

            while (buffer.hasRemaining()) {
                channel.write(buffer, position);
            }
            position += read;
        }
    }

    private void writeToMessage(ObjectOutput out, FileChannel channel,
            long position, long limit) throws IOException {
        byte[] bytes = new byte[32 * 1000];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        if (limit > size()) {
            throw new IOException("cannot write past end of file");
        }

        while (position < limit) {
            int maxRead = (int) Math.min((limit - position), bytes.length);

            buffer.position(0);
            buffer.limit(maxRead);

            channel.read(buffer, position);
            int read = buffer.position();

            out.writeArray(bytes, 0, read);
            position += read;
        }
    }

    protected void download(long position, long limit, FileChannel channel) {
        int blockPosition = blockPosition(position);
        int blockLimit = blockLimit(limit);

        int triesLeft = Config.FILE_DOWNLOAD_ATTEMPTS;

        while (triesLeft > 0) {
            Call call;
            BitSet requestedBlocks = new BitSet();

            requestedBlocks.set(blockPosition, blockLimit);
            synchronized (this) {
                requestedBlocks.andNot(availableBlocks);
            }

            // always the case at the primary node
            if (requestedBlocks.isEmpty()) {
                return;
            }

            try {
//                if (triesLeft < Config.FILE_PRIMARY_DOWNLOAD_TRESHOLD) {
                if(true) {
                    call = endPoint.call(primary);
                } else {
                    IbisIdentifier victim = job.getRandomConstituent();
                    call = endPoint.call(victim, id().toString());
                }

                call.writeInt(REQUEST_BLOCKS);
                call.writeObject(requestedBlocks);
                call.call(Config.CALL_TIMEOUT);

                int block = call.readInt();

                if (block == -1) {
                    // block not found at peer
                    call.finish();
                    triesLeft--;
                    continue;
                }

                long filePosition = position(block);
                long fileLimit = limit(block);

                if (fileLimit > size) {
                    fileLimit = size;
                }

                readFromMessage(call, channel, filePosition, fileLimit);
                call.finish();

                if (checkBlock(block, channel, filePosition, fileLimit)) {
                    synchronized (this) {
                        availableBlocks.set(block);
                        requestedBlocks.clear(block);
                    }
                }

            } catch (Exception e) {
                job.log("exeption on downloading file", e);
                triesLeft--;
            }
        }
    }

    private boolean checkBlock(int block, FileChannel channel,
            long filePosition, long fileLimit) throws IOException {

        Hash hash = new Hash(channel, filePosition, fileLimit);

        return hash.equals(hashes[block]);
    }

    public void invoke(Invocation invocation) throws ZorillaException,
            IOException {
        int opcode = invocation.readInt();
        BitSet requestedBlocks;

        if (opcode != REQUEST_BLOCKS) {
            throw new ZorillaException("unknown opcode in file request: "
                    + opcode);
        }

        try {
            requestedBlocks = (BitSet) invocation.readObject();
        } catch (ClassNotFoundException e) {
            throw new ZorillaException("class not found on reading request");
        }

        invocation.finishRead();

        synchronized (this) {
            // fileter out unavailable blocks
            requestedBlocks.and(availableBlocks);
        }

        if (requestedBlocks.isEmpty()) {
            // requested block(s) not found
            invocation.writeInt(-1);
            return;

        }

        int block = -1;
        while (block == -1) {
            block = random.nextInt(requestedBlocks.length());
            if (!requestedBlocks.get(block)) {
                block = -1;
            }
        }

        invocation.writeInt(block);

        long position = position(block);
        long limit = limit(block);
        if (limit > size()) {
            limit = size();
        }

        FileChannel channel = createFileChannel(file);

        writeToMessage(invocation, channel, position, limit);

        channel.close();
    }

    public void close() throws ZorillaException {
        try {
            endPoint.close();
        } catch (IOException e) {
            throw new ZorillaException("could not close endpoint", e);
        }
    }
    
    public String toString() {
        return path + "=" + file;
    }

}
