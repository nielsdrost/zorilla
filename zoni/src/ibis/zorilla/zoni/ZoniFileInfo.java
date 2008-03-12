package ibis.zorilla.zoni;

import java.io.IOException;

public class ZoniFileInfo {

    private final String sandboxPath;

    private final String name;

    private final boolean isDirectory;

    private final ZoniFileInfo[] children;

    public ZoniFileInfo(String sandboxPath, String name, boolean isDirectory,
            ZoniFileInfo[] children) {
        this.sandboxPath = sandboxPath;
        this.name = name;
        this.isDirectory = isDirectory;
        this.children = children.clone();
    }

    public ZoniFileInfo(ZoniInputStream in) throws IOException {
        sandboxPath = in.readString();
        name = in.readString();
        isDirectory = in.readBoolean();
        children = new ZoniFileInfo[in.readInt()];

        // recursive :)
        for (int i = 0; i < children.length; i++) {
            children[i] = new ZoniFileInfo(in);
        }
    }

    public void writeTo(ZoniOutputStream out) throws IOException {
        out.writeString(sandboxPath);
        out.writeString(name);
        out.writeBoolean(isDirectory);

        out.writeInt(children.length);
        // recursive :)
        for (ZoniFileInfo file : children) {
            file.writeTo(out);
        }
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getName() {
        return name;
    }

    public String getSandboxPath() {
        return sandboxPath;
    }

    public ZoniFileInfo[] getChildren() {
        return children.clone();
    }

    public String toString() {
        return "FileInfo: sandboxPath = " + sandboxPath + ", name = " + name
                + ", isDirectory = " + isDirectory + ", children length = "
                + children.length;
    }

}
