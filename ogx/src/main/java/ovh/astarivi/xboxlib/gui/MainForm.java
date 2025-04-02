package ovh.astarivi.xboxlib.gui;

import org.tinylog.Logger;
import ovh.astarivi.xboxlib.core.storage.AppProperties;
import ovh.astarivi.xboxlib.core.storage.PersistenceRepository;
import ovh.astarivi.xboxlib.gui.utils.GuiConfig;
import ovh.astarivi.xboxlib.gui.utils.JComboListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class MainForm {
    private JFrame frame;
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

        frame = new JFrame("OGXRepacker");
        frame.setIconImage(
                Toolkit.getDefaultToolkit().getImage(
                        getClass().getResource(
                                "/ovh/astarivi/xboxlib/res/icon.png"
                        )
                )
        );
        frame.setMinimumSize(new Dimension(570, 301));
        frame.setContentPane(rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Center the window
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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
                "DriveImageUtils (Legacy)",
                "None"
        }));

        attacherCombo.setSelectedIndex(
                appProperties.getIntProperty("attacher", 0)
        );

        attacherCombo.addItemListener(new JComboListener("attacher"));

        packCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                "XISO Auto",
                "XISO Rebuild",
                "XISO Trim",
                "XISO",
                "Extract only"
//                "CISO (CSO)"
        }));

        packCombo.addItemListener(this::packComboChanged);

        packCombo.setSelectedIndex(
                appProperties.getIntProperty("pack", 0)
        );

        splitCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                "Split at FATX limit",
                "Split in half",
                "Do not split"
        }));

        splitCombo.setSelectedIndex(
                appProperties.getIntProperty("split", 0)
        );

        splitCombo.addItemListener(new JComboListener("split"));

        inputField.setText(
                appProperties.getProperty("input", "")
        );

        inputField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                appProperties.setProperty("input", inputField.getText());
                appProperties.save();
            }
        });

        outputField.setText(
                appProperties.getProperty("output", "")
        );

        outputField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                appProperties.setProperty("output", outputField.getText());
                appProperties.save();
            }
        });

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

        openInputButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(inputField.getText()));
            } catch (Exception ignored) {
            }
        });

        processButton.addActionListener(this::startProcess);
    }

    private void saveSettings(){
        PersistenceRepository.getInstance().getAppProperties().save();
    }

    private void packComboChanged(ItemEvent e) {
        new JComboListener("pack").itemStateChanged(e);

        GuiConfig.Pack pack = GuiConfig.Pack.getFromValue(packCombo.getSelectedIndex());

        // EXTRACT doesn't support these
        if (pack == GuiConfig.Pack.EXTRACT){
            attacherCombo.setEnabled(false);
            splitCombo.setEnabled(false);
        } else {
            splitCombo.setEnabled(true);
            attacherCombo.setEnabled(true);
        }
    }

    // Lots of user checks
    private void startProcess(ActionEvent e) {
        String inputFieldText = inputField.getText();
        String outputFieldText = outputField.getText();

        if (inputFieldText == null || inputFieldText.isEmpty()) {
            JOptionPane.showMessageDialog(
                    rootPanel,
                    "Input path field cannot be empty.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (outputFieldText == null || outputFieldText.isEmpty()) {
            JOptionPane.showMessageDialog(
                    rootPanel,
                    "Output path field cannot be empty.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        Path inputPath = Path.of(inputFieldText);

        if (!Files.isDirectory(inputPath)) {
            JOptionPane.showMessageDialog(
                    rootPanel,
                    "Input path does not exist, or is not a readable directory.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        Path outputPath = Path.of(outputFieldText);

        if (!Files.isDirectory(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException ex) {
                Logger.error("Failed to create folder {}", outputPath.toString());
                Logger.error(ex);
                JOptionPane.showMessageDialog(
                        rootPanel,
                        "Output folder does not exist, and we could not create it.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        if (Files.isRegularFile(outputPath)) {
            JOptionPane.showMessageDialog(
                    rootPanel,
                    "Output path should be a folder, not a file.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (inputPath.toAbsolutePath() == outputPath.toAbsolutePath()) {
            JOptionPane.showMessageDialog(
                    rootPanel,
                    "Input folder cannot be the same as the output folder.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        GuiConfig guiConfig = new GuiConfig(
                inputPath,
                outputPath,
                namingCombo.getSelectedIndex(),
                attacherCombo.getSelectedIndex(),
                packCombo.getSelectedIndex(),
                splitCombo.getSelectedIndex()
        );

        SwingUtilities.invokeLater(() -> {
            ProgressForm pForm = new ProgressForm(frame, guiConfig);
            pForm.start();
        });
    }
}
