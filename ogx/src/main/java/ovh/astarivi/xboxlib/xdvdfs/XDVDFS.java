package ovh.astarivi.xboxlib.xdvdfs;

import org.tinylog.Logger;
import ovh.astarivi.xboxlib.xdvdfs.base.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class XDVDFS {
    static {
        System.loadLibrary("xdvdfs_jlib");
    }
    private final String source;
    private final String destination;
    private XDVDFSListener listener = null;

    public XDVDFS(Path sourcePath, Path destinationPath) throws IOException {
        this.source = sourcePath.toRealPath().toString();
        this.destination = destinationPath.toRealPath().toString();

        Logger.info("Source: {}, Destination: {}", source, destination);

        Logger.info(
                "{} source is a {}",
                source,
                Files.isRegularFile(sourcePath) ? "file" : "directory"
        );
    }

    // Native method
    private native void pack(String source, String destination);

    // Called from Rust
    @SuppressWarnings("unused")
    private void callback(String message, int status) {
        Logger.info("xdvdfs: {}, {}", message, status);

        if (listener == null) return;

        try {
            switch (XDVDFSStatus.getFromValue(status)) {
                case EXTRACTING_FILES:
                    listener.onProgress(message);
                    break;
                case ERROR:
                    listener.onError(new XDVDFSException(message));
                    break;
                case FINISHED:
                    listener.onFinished();
                    break;
            }
        } catch(Exception e) {
            Logger.error("Rust callback encountered an error.");
            Logger.error(e);
        }
    }

    public void setListener(XDVDFSListener listener) {
        this.listener = listener;
    }

    public void start() {
        pack(source, destination);
    }
}