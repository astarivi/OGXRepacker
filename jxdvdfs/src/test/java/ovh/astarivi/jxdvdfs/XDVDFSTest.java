package ovh.astarivi.jxdvdfs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;
import ovh.astarivi.jxdvdfs.base.XDVDFSException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
//
//        assertEquals("Chainsaw Man", anime.attributes.canonicalTitle);
//        assertEquals("Chainsaw Man", anime.attributes.titles.en);
//        assertEquals("2022-10-11", anime.attributes.startDate);
    }
}
