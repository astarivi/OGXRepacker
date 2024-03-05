package ovh.astarivi.xboxlib;

import org.tinylog.Logger;
import ovh.astarivi.xboxlib.xdvdfs.XDVDFS;
import ovh.astarivi.xboxlib.xdvdfs.base.XDVDFSException;
import ovh.astarivi.xboxlib.xdvdfs.base.XDVDFSListener;

import java.io.IOException;
import java.nio.file.Path;


public class Main {
    public static void main(String[] args) throws IOException {
        XDVDFS xdvdfs = new XDVDFS(
                Path.of("D:\\xbox\\test\\redump.iso"),
                Path.of("D:\\xbox\\test\\output.iso")
        );
        xdvdfs.setListener(new XDVDFSListener() {
            @Override
            public void onError(XDVDFSException exception) {
                Logger.info(exception);
            }

            @Override
            public void onProgress(String status) {
                Logger.info(status);
            }

            @Override
            public void onFinished() {
                Logger.info("XDVDFS: Done");
            }
        });
        xdvdfs.start();
    }
}
