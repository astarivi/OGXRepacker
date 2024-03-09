package ovh.astarivi.xboxlib.core.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


public class OGXArchive {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Game {
        public String title_id;
        public String title;
        public String iso_name;
    }

    public static Map<String, Game> loadArchive() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream inputStream = OGXArchive.class.getClassLoader().getResourceAsStream("archive.json")) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        }
    }
}
