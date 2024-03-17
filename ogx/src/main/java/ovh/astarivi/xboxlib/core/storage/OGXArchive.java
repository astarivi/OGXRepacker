package ovh.astarivi.xboxlib.core.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;
import ovh.astarivi.xboxlib.core.utils.Utils;
import ovh.astarivi.xboxlib.core.xbe.XBE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;


public class OGXArchive {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Game {
        public String title_id;
        public String title;
        public String iso_name;
        public String xbe_title;

        public Game() {
        }

        @JsonIgnore
        public Game(String title_id, String title, String iso_name, String xbe_title) {
            this.title_id = title_id;
            this.title = title;
            this.iso_name = iso_name;
            this.xbe_title = xbe_title;
        }

        @JsonIgnore
        public static @Nullable OGXArchive.Game retrieve(String titleId) {
            return PersistenceRepository.getInstance().getGamesArchive().get(
                    titleId
            );
        }

        @JsonIgnore
        public static OGXArchive.Game buildDummy(XBE xbe, Path entry) {
            String entryFilename = Utils.toFatXFilename(
                    Utils.removeFileExtension(
                            entry.getFileName().toString(),
                            true
                    )
            );

            return new OGXArchive.Game(
                    xbe.cert.dwTitleId,
                    entryFilename.substring(0, Math.min(entryFilename.length(), 42)),
                    entryFilename.substring(0, Math.min(entryFilename.length(), 36)),
                    entryFilename.substring(0, Math.min(entryFilename.length(), 40))
            );
        }
    }

    public static Map<String, Game> loadArchive() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream inputStream = OGXArchive.class.getClassLoader().getResourceAsStream("archive.json")) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        }
    }
}
