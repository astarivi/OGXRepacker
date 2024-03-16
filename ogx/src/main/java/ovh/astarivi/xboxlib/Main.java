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
        startGUI();
    }
}
