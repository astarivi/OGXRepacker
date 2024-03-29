package ovh.astarivi.xboxlib;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMoonlightIJTheme;
import ovh.astarivi.xboxlib.gui.MainForm;

import javax.swing.*;


public class Main {
    public static void startGUI() {
        FlatMoonlightIJTheme.setup();

        SwingUtilities.invokeLater(MainForm::new);
    }

    public static void main(String[] args) {
        startGUI();
    }
}
