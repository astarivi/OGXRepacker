package ovh.astarivi.xboxlib.gui;

import lombok.Getter;
import org.tinylog.Logger;
import ovh.astarivi.xboxlib.core.Threading;
import ovh.astarivi.xboxlib.core.utils.Utils;
import ovh.astarivi.xboxlib.gui.utils.GuiConfig;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@Getter
public class ProgressForm {
    private final GuiConfig config;
    private Future<?> task = null;
    private JDialog dialog;
    private JPanel rootPanel;
    private JTextArea logsText;
    private JProgressBar totalProgress;
    private JProgressBar currentProgress;
    private JButton cancelButton;

    public ProgressForm(JFrame parentFrame, GuiConfig config) {
        this.config = config;
        dialog = new JDialog(parentFrame, "Repacking progress", true);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setContentPane(rootPanel);
        dialog.pack();
        dialog.setResizable(false);
        // Center the window
        dialog.setLocationRelativeTo(null);

        logsText.setEditable(false);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (task != null && !task.isDone()) {
                    task.cancel(true);
                }
                GuiReference.progressForm.set(null);
            }
        });

        cancelButton.addActionListener(e -> {
            int selection = JOptionPane.showConfirmDialog(
                    dialog,
                    "Are you sure you want to cancel the process?",
                    "Confirmation",
                    JOptionPane.YES_NO_OPTION
            );

            if (selection == JOptionPane.YES_OPTION) {
                dialog.dispose();
            }
        });
    }

    public void addEvent(String event) {
        logsText.append(event + "\n");
        logsText.setCaretPosition(logsText.getText().length());

    }

    public void processError(String error) {
        JOptionPane.showMessageDialog(
                dialog,
                error + "\n...Canceling...",
                "Fatal error",
                JOptionPane.ERROR_MESSAGE
        );

        finish();
    }

    public void finish() {
        cancelButton.setText("Ok");

        for (ActionListener e : cancelButton.getActionListeners()) {
            cancelButton.removeActionListener(e);
        }

        cancelButton.addActionListener(e -> dialog.dispose());
    }

    public void start() {
        addEvent("Initializing OGXRepacker");

        task = Threading.getInstance().getProcessExecutor().submit(() -> {
            totalProgress.setIndeterminate(true);
            currentProgress.setIndeterminate(true);
            addEvent("Scanning input folder for files/folders to process");

            ArrayList<Path> inputItems = new ArrayList<>();

            try(Stream<Path> walkStream = Files.walk(config.inputField())) {
                walkStream
                        .filter(path -> {
                            // ISO file
                            if (Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".iso")) {
                                return true;
                            }

                            // Dir
                            return Files.isDirectory(path) && Utils.containsDefaultXbe(path);
                        })
                        .forEach(inputItems::add);
            } catch (IOException e) {
                Logger.error("Failed to walk path {}", config.inputField().toString());
                Logger.error(e);
                processError("Failed to walk input path");
                return;
            }

            if (inputItems.isEmpty()) {
                addEvent("No valid inputs found");
                totalProgress.setIndeterminate(false);
                currentProgress.setIndeterminate(false);
                totalProgress.setValue(100);
                finish();
                return;
            }

            totalProgress.setIndeterminate(false);

            int totalEntries = inputItems.size();
            addEvent("Found %d valid entries to process".formatted(totalEntries));
            int currentEntry = 0;

            for (Path entry : inputItems) {
                currentEntry++;

                addEvent("Processing entry no. %d, %s".formatted(
                        currentEntry, entry.getFileName().toString()
                ));

                addEvent("Reading file metadata");
            }
        });

        dialog.setVisible(true);
    }
}
