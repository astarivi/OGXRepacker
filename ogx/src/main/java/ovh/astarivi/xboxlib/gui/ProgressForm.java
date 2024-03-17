package ovh.astarivi.xboxlib.gui;

import lombok.Getter;
import ovh.astarivi.xboxlib.core.Pack;
import ovh.astarivi.xboxlib.core.Threading;
import ovh.astarivi.xboxlib.core.utils.SmartScroller;
import ovh.astarivi.xboxlib.core.utils.Utils;
import ovh.astarivi.xboxlib.gui.utils.GuiConfig;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Future;


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
    private JScrollPane logsScrollPane;

    public ProgressForm(JFrame parentFrame, GuiConfig config) {
        this.config = config;
        dialog = new JDialog(parentFrame, "OGXRepacker - Repacking in progress", true);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setContentPane(rootPanel);
        dialog.pack();
        dialog.setResizable(false);
        // Center the window
        dialog.setLocationRelativeTo(null);
        new SmartScroller(logsScrollPane);

        logsText.setEditable(false);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (task != null && !task.isDone()) {
                    task.cancel(true);
                }

                try {
                    Utils.cleanTemp();
                } catch (Exception ignored) {
                }
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

        task = Threading.getInstance().getProcessExecutor().submit(
                new Pack(config, this)
        );

        dialog.setVisible(true);
    }
}
