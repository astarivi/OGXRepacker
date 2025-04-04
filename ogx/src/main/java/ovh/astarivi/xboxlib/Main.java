package ovh.astarivi.xboxlib;

import com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme;
import ovh.astarivi.xboxlib.gui.MainForm;
import ovh.astarivi.xboxlib.gui.utils.PlatformUtils;

import javax.swing.*;


public class Main {
    public static void startGUI() {
        PlatformUtils.setupLogging();

        FlatSolarizedLightIJTheme.setup();

        SwingUtilities.invokeLater(MainForm::new);
    }

    public static void main(String[] args) {
        startGUI();
    }
}
