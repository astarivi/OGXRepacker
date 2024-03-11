package ovh.astarivi.xboxlib.core.utils;

import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {
    public static boolean containsDefaultXbe(Path directory) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, entry ->
            entry.getFileName().toString().equalsIgnoreCase("default.xbe")
        )) {
            // Check if any matching files were found
            return stream.iterator().hasNext();
        } catch (IOException e) {
            Logger.error("Error while walking {} for default.xbe", directory.toString());
            Logger.error(e);
        }
        return false;
    }
}