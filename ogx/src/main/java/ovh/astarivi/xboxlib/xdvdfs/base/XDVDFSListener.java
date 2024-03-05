package ovh.astarivi.xboxlib.xdvdfs.base;


public interface XDVDFSListener {
    void onError(XDVDFSException exception);
    void onProgress(String status);
    void onFinished();
}
