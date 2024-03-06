package ovh.astarivi.xboxlib.gui;

import javax.swing.*;


public class MainForm {
    private JTextField inputField;
    private JButton inputBtn;
    private JTextField outputField;
    private JButton outputBtn;
    private JComboBox packCombo;
    private JButton processButton;
    private JButton openInputButton;
    public JPanel rootPanel;
    private JPanel childPanel;
    private JComboBox splitCombo;
    private JComboBox attacherCombo;
    private JComboBox namingCombo;

    public MainForm() {
        namingCombo.setModel(new DefaultComboBoxModel(new String[]{
                "XBE Name",
                "Repackinator",
                "Keep Original"
        }));
    }
}
