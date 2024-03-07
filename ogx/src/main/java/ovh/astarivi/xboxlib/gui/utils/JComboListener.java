package ovh.astarivi.xboxlib.gui.utils;

import ovh.astarivi.xboxlib.core.storage.AppProperties;
import ovh.astarivi.xboxlib.core.storage.PersistenceRepository;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


public class JComboListener implements ItemListener {
    private final String saveKey;

    public JComboListener(String saveKey) {
        this.saveKey = saveKey;
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            @SuppressWarnings("unchecked") JComboBox<String> comboBox = (JComboBox<String>) e.getSource();

            AppProperties properties = PersistenceRepository.getInstance().getAppProperties();
            properties.setIntProperty(this.saveKey, comboBox.getSelectedIndex());
            properties.save();
        }
    }
}
