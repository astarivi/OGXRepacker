package ovh.astarivi.jxdvdfs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;
import ovh.astarivi.jxdvdfs.base.XDVDFSException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class XDVDFSTest {

    @BeforeAll
    static void setup() {
        Path debugLibraryPath = Paths.get("")
                .toAbsolutePath()
                .getParent()
                .resolve("xdvdfs-jlib")
                .resolve("target")
                .resolve("debug")
                .resolve(System.mapLibraryName("xdvdfs_jlib"));

        new XDVDFS(debugLibraryPath.toString());
    }

    @Test
    @DisplayName("XDVDFS pack")
    void testPack() throws IOException, XDVDFSException {
        XDVDFS xdvdfs = new XDVDFS();
        xdvdfs.setPackListener(Logger::info);

        xdvdfs.pack(
                Path.of("D:\\xbox\\test\\redump.iso"),
                Path.of("D:\\xbox\\test\\output.iso")
        );
    }

    @Test
    @DisplayName("XDVDFS CISO")
    void testCISO() throws XDVDFSException {
        XDVDFS xdvdfs = new XDVDFS();
        xdvdfs.setPackListener(Logger::info);

        xdvdfs.compressCISO(
                Path.of("D:\\xbox\\test\\redump.iso"),
                Path.of("D:\\xbox\\test\\output.cso"),
                524288000,
                true
        );
    }

    @Test
    @DisplayName("XDVDFS CISO Interruptibility")
    public void testCompressInterruptibility() throws Exception {
        Thread worker = new Thread(() -> {
            try {
                XDVDFS xdvdfs = new XDVDFS();
                xdvdfs.setPackListener(Logger::info);

                xdvdfs.compressCISO(
                        Path.of("D:\\xbox\\test\\redump.iso"),
                        Path.of("D:\\xbox\\test\\output.cso"),
                        524_288_000,
                        true
                );
            } catch (Exception e) {
                Logger.info(e);
            }
        });

        long start = System.currentTimeMillis();
        worker.start();

        // Wait 3 seconds then interrupt
        Thread.sleep(3000);
        worker.interrupt();

        worker.join(5000);

        long duration = System.currentTimeMillis() - start;

        assertFalse(worker.isAlive(), "Thread should have stopped after interrupt.");
        assertTrue(duration < 10_000, "Thread did not stop within 5 seconds.");
    }

    @Test
    @DisplayName("XDVDFS stat")
    void testStat() throws IOException, XDVDFSException {
        XDVDFS xdvdfs = new XDVDFS();

        Logger.info(
                xdvdfs.stat(
                        Path.of("D:\\xbox\\test\\redump.iso")
                )
        );
    }

    @Test
    @DisplayName("XDVDFS file unpack")
    void testFileUnpack() throws IOException, XDVDFSException {
        XDVDFS xdvdfs = new XDVDFS();

        xdvdfs.unpackFile(
                Path.of("D:\\xbox\\test\\redump.iso"),
                Path.of("D:\\xbox\\test\\default.xbe"),
                "/default.xbe"
        );
    }

    @Test
    @DisplayName("XDVDFS split pack")
    void testSplitPack() throws Exception{
        XDVDFS xdvdfs = new XDVDFS();

        xdvdfs.packSplit(
                Path.of("D:\\xbox\\test\\redump.iso"),
                Path.of("D:\\xbox\\test\\output.iso"),
                619430400
        );
    }
}
