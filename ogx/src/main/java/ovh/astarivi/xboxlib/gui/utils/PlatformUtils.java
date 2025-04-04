package ovh.astarivi.xboxlib.gui.utils;

import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;
import ovh.astarivi.xboxlib.core.storage.PersistenceRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class PlatformUtils {
    public static @NotNull Path getPersistentFolder(String appName) {
        String os = System.getProperty("os.name").toLowerCase(Locale.US);

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                return Paths.get(appData, appName);
            }
        } else if (os.contains("mac")) {
            String home = System.getProperty("user.home");
            if (home != null && !home.isBlank()) {
                return Paths.get(home, "Library", "Application Support", appName);
            }
        } else {
            String xdgConfig = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfig != null && !xdgConfig.isBlank()) {
                return Paths.get(xdgConfig, appName);
            }

            try {
                String home = System.getProperty("user.home");
                if (home != null && !home.isBlank()) {
                    return Paths.get(home, ".config", appName);
                }
            } catch(SecurityException ignored) {
            }
        }

        // Subfolder fallback
        Path currDir = Paths.get("").toAbsolutePath();
        return currDir.resolve(appName);
    }

    public static void setupLogging() {
        Path logDir = PersistenceRepository.persistenceFolder.resolve("logs");

        Configuration.set("writer2", "console");
        Configuration.set("writer2.level", "trace");

        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            Logger.error("Failed to setup logging to folder {}", logDir.toString());
        }

        Configuration.set("writer", "rolling file");
        Configuration.set("writer.file", logDir.resolve("{date}.log").toString());
        Configuration.set("writer.latest", logDir.resolve("latest.log").toString());
        Configuration.set("writer.level", "info");
    }
}
