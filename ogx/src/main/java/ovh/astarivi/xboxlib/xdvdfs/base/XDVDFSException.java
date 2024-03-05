package ovh.astarivi.xboxlib.xdvdfs.base;

public class XDVDFSException extends Exception{
    public XDVDFSException() {
    }

    public XDVDFSException(String message) {
        super(message);
    }

    public XDVDFSException(String message, Throwable cause) {
        super(message, cause);
    }

    public XDVDFSException(Throwable cause) {
        super(cause);
    }
}
