package ovh.astarivi.jxdvdfs.base;

import org.tinylog.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;


public class NativeLoader {
    public static void load(String name) {
        String currentOs = System.getProperty("os.name").toLowerCase(Locale.US);
        String effectiveOs = currentOs.contains(" ") ? currentOs.split(" ")[0].trim() : currentOs;

        HashMap<String, String> archMap = new HashMap<>();
        archMap.put("x86", "i686");
        archMap.put("i386", "i686");
        archMap.put("i486", "i686");
        archMap.put("i586", "i686");
        archMap.put("i686", "i686");
        archMap.put("x86_64", "x86_64");
        archMap.put("amd64", "x86_64");
        archMap.put("arm", "arm");
        archMap.put("armv5te", "arm");
        archMap.put("armv6", "arm");
        archMap.put("armv7", "arm");
        archMap.put("aarch64", "aarch64");
        archMap.put("arm64", "aarch64");
        archMap.put("riscv", "riscv");
        archMap.put("riscv64", "riscv");
        archMap.put("riscv64gc", "riscv");

        String effectiveArch = archMap.get(
                System.getProperty("os.arch").toLowerCase(Locale.US)
        );

        if (effectiveArch == null) {
            // Emulation support
            effectiveArch = "x86_64";
        }

        String libraryName = System.mapLibraryName(name);

        String effectivePath = "%s/%s/%s".formatted(
                effectiveOs,
                effectiveArch,
                libraryName
        );

        try (InputStream inputStream = NativeLoader.class.getClassLoader().getResourceAsStream(effectivePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Couldn't find %s inside the .jar".formatted(effectiveOs));
            }

            Path tempFile = Files.createTempFile(libraryName, "");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            System.load(tempFile.toAbsolutePath().toString());
        } catch (IOException e) {
            Logger.error("Failed to find native lib file {} inside .jar", effectivePath);
            Logger.error("OS: {}, Arch: {}", System.getProperty("os.name"), System.getProperty("os.arch"));
            throw new RuntimeException(e);
        }
    }
}
