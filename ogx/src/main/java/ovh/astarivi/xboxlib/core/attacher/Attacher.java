package ovh.astarivi.xboxlib.core.attacher;

import org.tinylog.Logger;
import ovh.astarivi.xboxlib.core.storage.OGXArchive;
import ovh.astarivi.xboxlib.core.xbe.XBE;
import ovh.astarivi.xboxlib.gui.utils.GuiConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;


public class Attacher {
    private final XBE defaultXbe;
    private final OGXArchive.Game game;
    private final Path outputFile;

    public Attacher(XBE defaultXbe, OGXArchive.Game game, Path outputFile) {
        this.defaultXbe = defaultXbe;
        this.game = game;
        this.outputFile = outputFile;
    }

    public void create(GuiConfig.Attacher attacher, GuiConfig.Naming naming) throws IOException {
        String attacherRoute = switch (attacher) {
            case CERBIOS -> "attacher/cerbios.xbe";
            case STELLAR -> "attacher/stellar.xbe";
            case DRIVE_IMAGE_UTILS -> "attacher/driveimageutils.xbe";
            default -> throw new IllegalArgumentException("Invalid attacher value");
        };

        try (InputStream inputStream = Attacher.class.getClassLoader().getResourceAsStream(attacherRoute)) {
            Files.copy(Objects.requireNonNull(inputStream), outputFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (NullPointerException e) {
            throw new IOException(e);
        }

        XBE attacherXbe = new XBE(outputFile);

        try (FileChannel axbe = FileChannel.open(outputFile, StandardOpenOption.WRITE)) {
            int certAddress = attacherXbe.header.dwCertificateAddr - attacherXbe.header.dwBaseAddr;
            ByteBuffer ogCert = ByteBuffer.wrap(defaultXbe.rawCert).order(ByteOrder.LITTLE_ENDIAN);

            int written = axbe.write(ogCert, certAddress);

            if (written != 464) {
                Logger.error("Incomplete attacher certificate write for {}", defaultXbe.cert.dwTitleId);
            }

            if (naming == GuiConfig.Naming.KEEP_FILENAME) return;

            byte[] encodedBytes = game.xbe_title.getBytes(StandardCharsets.UTF_16LE);
            byte[] titleBytes = new byte[80];

            System.arraycopy(encodedBytes, 0, titleBytes, 0, Math.min(encodedBytes.length, titleBytes.length));

            axbe.write(
                    ByteBuffer.wrap(titleBytes).order(ByteOrder.LITTLE_ENDIAN),
                    certAddress + 12
            );
        }
    }
}
