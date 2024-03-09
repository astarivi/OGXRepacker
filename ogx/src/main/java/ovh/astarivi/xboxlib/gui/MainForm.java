package ovh.astarivi.xboxlib.gui;

import ovh.astarivi.xboxlib.core.storage.AppProperties;
import ovh.astarivi.xboxlib.core.storage.PersistenceRepository;
import ovh.astarivi.xboxlib.gui.utils.JComboListener;

import javax.swing.*;


public class MainForm {
    private JTextField inputField;
    private JButton inputBtn;
    private JTextField outputField;
    private JButton outputBtn;
    private JButton processButton;
    private JButton openInputButton;
    public JPanel rootPanel;
    private JPanel childPanel;
    private JComboBox<String> packCombo;
    private JComboBox<String> splitCombo;
    private JComboBox<String> attacherCombo;
    private JComboBox<String> namingCombo;

    // Initialize this, and load settings
    public MainForm() {
        loadValues();
    }

    private void loadValues() {
        AppProperties appProperties = PersistenceRepository.getInstance().getAppProperties();

        namingCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                "OGXRepacker",
                "Repackinator",
                "Keep filename"
        }));

        namingCombo.setSelectedIndex(
                appProperties.getIntProperty("naming", 0)
        );

        namingCombo.addItemListener(new JComboListener("naming"));

        attacherCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                "Cerbios",
                "Stellar",
                "DriveImageUtils",
                "Legacy",
                "None"
        }));

        attacherCombo.setSelectedIndex(
                appProperties.getIntProperty("attacher", 0)
        );

        attacherCombo.addItemListener(new JComboListener("attacher"));

        packCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                "XDVDFS (XISO)",
                "CISO (CSO)"
        }));

        packCombo.setSelectedIndex(
                appProperties.getIntProperty("pack", 0)
        );

        packCombo.addItemListener(new JComboListener("pack"));

        splitCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                "Split in half",
//                "Split at FATX limit",
                "Do not split"
        }));

        splitCombo.setSelectedIndex(
                appProperties.getIntProperty("split", 0)
        );

        splitCombo.addItemListener(new JComboListener("split"));

        inputField.setText(
                appProperties.getProperty("input", "")
        );

        outputField.setText(
                appProperties.getProperty("output", "")
        );

        inputBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = fileChooser.showOpenDialog(rootPanel);

            if(option != JFileChooser.APPROVE_OPTION) {
                return;
            }

            String filename = fileChooser.getSelectedFile().toString();
            appProperties.setProperty("input", filename);

            inputField.setText(filename);
            saveSettings();
        });

        outputBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = fileChooser.showOpenDialog(rootPanel);

            if(option != JFileChooser.APPROVE_OPTION) {
                return;
            }

            String filename = fileChooser.getSelectedFile().toString();
            appProperties.setProperty("output", filename);

            outputField.setText(filename);
            saveSettings();
        });
    }

    private void saveSettings(){
        PersistenceRepository.getInstance().getAppProperties().save();
    }
}
