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
    public static class Game implements Cloneable{
        public String title_id;
        public String title;
        public String iso_name;
        public String xbe_title;

        @SuppressWarnings("unused")
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
            OGXArchive.Game g = PersistenceRepository.getInstance().getGamesArchive().get(
                    titleId
            );

            if (g == null) {
                return null;
            }

            return g.clone();
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

        @JsonIgnore
        @Override
        public Game clone() {
            try {
                return (Game) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public static Map<String, Game> loadArchive() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream inputStream = OGXArchive.class.getClassLoader().getResourceAsStream("archive.json")) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        }
    }
}
