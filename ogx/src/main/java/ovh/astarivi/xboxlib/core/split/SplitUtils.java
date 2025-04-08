package ovh.astarivi.xboxlib.core.split;

import org.tinylog.Logger;
import ovh.astarivi.xboxlib.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SplitUtils {
    private static final Pattern pattern = Pattern.compile("\\d+$");

    public static void rename(Path firstImage, String extension, boolean skipSingle) throws IOException {
        Path baseFolder = firstImage.getParent();
        String baseFileName = Utils.removeFileExtension(
                firstImage.getFileName().toString(),
                false
        );

        // Check if we have a singular part (no splitting took place)
        try(Stream<Path> fileStream = Files.list(baseFolder)) {
            if (skipSingle && fileStream.filter(Files::isRegularFile).count() == 1) return;
        }

        Files.move(firstImage, baseFolder.resolve(baseFileName + ".1.%s".formatted(extension)));

        try(Stream<Path> fileStream = Files.list(baseFolder)) {
            fileStream
                    .filter(path -> pattern.matcher(path.toString()).find())
                    .forEach(path -> {
                        Matcher matcher = pattern.matcher(path.toString());

                        if (!matcher.find()) return;

                        try {
                            Files.move(path, baseFolder.resolve(baseFileName + ".%s.%s".formatted(matcher.group(), extension)));
                        } catch (IOException e) {
                            Logger.error("Failed to rename {}", path);
                            Logger.error(e);
                        }
                    });
        }
    }
}
