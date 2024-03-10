package ovh.astarivi.xboxlib;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMoonlightIJTheme;
import ovh.astarivi.xboxlib.gui.MainForm;

import javax.swing.*;
import java.io.IOException;


public class Main {
    public static void startGUI() {
        FlatMoonlightIJTheme.setup();

        SwingUtilities.invokeLater(() -> {
            MainForm mainForm = new MainForm();
        });
    }

    public static void main(String[] args) throws IOException {
//        XDVDFS xdvdfs = new XDVDFS(
//                Path.of("D:\\xbox\\test\\redump.iso"),
//                Path.of("D:\\xbox\\test\\output.iso")
//        );
//        xdvdfs.setListener(new XDVDFSListener() {
//            @Override
//            public void onError(XDVDFSException exception) {
//                Logger.info(exception);
//            }
//
//            @Override
//            public void onProgress(String status) {
//                Logger.info(status);
//            }
//
//            @Override
//            public void onFinished() {
//                Logger.info("XDVDFS: Done");
//            }
//        });
//        xdvdfs.start();
        startGUI();
    }
}
