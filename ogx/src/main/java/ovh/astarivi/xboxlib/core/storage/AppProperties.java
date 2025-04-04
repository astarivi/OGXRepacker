package ovh.astarivi.xboxlib.core.storage;

import org.tinylog.Logger;

import java.io.*;
import java.util.Objects;
import java.util.Properties;


public class AppProperties extends Properties {
    private final String filename;
    private final File localFile;

    public AppProperties(String filename) {
        this.filename = filename;

        localFile = PersistenceRepository.persistenceFolder.resolve("config").resolve(filename).toFile();
        if (!localFile.exists()) {
            ensureDirectory();
            return;
        }

        Logger.info("Settings file path: {}", localFile);

        try (FileInputStream fileInputStream = new FileInputStream(localFile)){
            this.load(fileInputStream);
        } catch(FileNotFoundException e) {
            ensureDirectory();
        } catch(IllegalArgumentException | IOException e) {
            Logger.debug(e);
            if (!localFile.delete()) localFile.deleteOnExit();
            this.clear();
        }
    }

    private void ensureDirectory() {
        try {
            //noinspection ResultOfMethodCallIgnored
            Objects.requireNonNull(localFile.getParentFile()).mkdirs();
        } catch(NullPointerException ignored) {

        }
    }

    public synchronized void save(){
        ensureDirectory();

        try (FileOutputStream fileOutput = new FileOutputStream(localFile)){
            this.store(fileOutput, filename);
        } catch (IOException e) {
            Logger.debug(e);
            // Delete file if possible
            if (!localFile.delete()) localFile.deleteOnExit();
        } catch (ClassCastException e) {
            // Shouldn't ever happen, but let's make sure it's not a problem
            this.clear();
        }
    }

    public int getIntProperty(String key, int defaultValue) {
        String property = getProperty(key);

        try {
            return Integer.parseInt(Objects.requireNonNull(property));
        } catch (NullPointerException e) {
            return defaultValue;
        } catch (NumberFormatException e) {
            setIntProperty(key, defaultValue);
            return defaultValue;
        }
    }

    public void setIntProperty(String key, int defaultValue) {
        setProperty(key, Integer.toString(defaultValue));
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String property = getProperty(key);

        try {
            return parseBoolean(Objects.requireNonNull(property));
        } catch (NullPointerException e) {
            return defaultValue;
        } catch (IllegalArgumentException e) {
            setBooleanProperty(key, defaultValue);
            return defaultValue;
        }
    }

    public void setBooleanProperty(String key, boolean value) {
        setProperty(key, String.valueOf(value));
    }

    private boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        } else {
            throw new IllegalArgumentException("Value cannot be converted to boolean");
        }
    }
}
