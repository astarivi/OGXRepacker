package ovh.astarivi.xboxlib;

import com.formdev.flatlaf.FlatDarkLaf;
import ovh.astarivi.xboxlib.gui.MainForm;

import javax.swing.*;
import java.io.IOException;


public class Main {
    public static void startGUI() {
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("OGXRepacker");
            MainForm mainForm = new MainForm();
            frame.setContentPane(mainForm.rootPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
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
