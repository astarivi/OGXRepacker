package ovh.astarivi.xboxlib.xdvdfs;


public class XDVDFS {
    static {
        System.loadLibrary("xdvdfs_jlib");
    }
    private native void pack(String source, String destination);

    private void callback(String message, int status) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        XDVDFS xdvdfs = new XDVDFS();
        xdvdfs.pack("D:\\xbox\\test\\redump.iso", "D:\\xbox\\test\\output.iso");
        System.out.println("Done from Java!");
    }
}