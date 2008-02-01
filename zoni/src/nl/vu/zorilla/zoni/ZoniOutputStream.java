package nl.vu.zorilla.zoni;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

public class ZoniOutputStream extends DataOutputStream {

    public ZoniOutputStream(OutputStream out) {
        super(out);
    }

    public void writeStringMap(Map map) throws IOException {
        writeInt(map.size());

        Iterator iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();

            writeString((String) entry.getKey());
            writeString((String) entry.getValue());
        }
    }

    public void writeString(String string) throws IOException {
        if (string == null) {
            writeUTF("<<null>>");
        } else if (string.equals("<<null>>")) {
            throw new IOException("cannot write reserved string \"<<null>>\"");
        } else {
            writeUTF(string);
        }
    }

    public void writeStrings(String[] strings) throws IOException {
        writeInt(strings.length);

        for (int i = 0; i < strings.length; i++) {
            writeString(strings[i]);
        }
    }

    public void writeFile(InputStream file) throws IOException {
        byte[] buffer = new byte[ZoniProtocol.MAX_BLOCK_SIZE];
        while (true) {
            int bytesRead = file.read(buffer);

            if (bytesRead == -1) {
                writeInt(-1);
                return;
            }

            write(buffer, 0, bytesRead);
        }
    }

}
