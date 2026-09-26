package com.dealerlink.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * All JSON work in DealerLink goes through this one class and one shared Jackson
 * {@link ObjectMapper} (ObjectMapper is thread-safe and expensive to create, so it
 * is built once).
 *
 *  - load()           : reads the EXTERNAL file data/dealerlink-data.json into SeedData
 *  - settings()       : app-wide settings from that file (threshold, intervals, currency...)
 *  - exportToFile()   : writes any list/object (e.g. orders) as pretty-printed JSON
 *  - mapper()         : shared mapper, also used by WeatherService to parse the REST API
 *
 * File location: "data/dealerlink-data.json" relative to the folder the app is started
 * from (the project root when using mvn javafx:run). Override with
 *   -Ddealerlink.data=/full/path/to/file.json
 */
public final class JsonDataService {

    public static final String DEFAULT_PATH = "data/dealerlink-data.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            // tolerate extra keys in the JSON (e.g. comments-as-fields, future fields)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static SeedData cached;

    private JsonDataService() {}

    public static ObjectMapper mapper() { return MAPPER; }

    /** Path of the external JSON file actually used. */
    public static Path dataFile() {
        return Paths.get(System.getProperty("dealerlink.data", DEFAULT_PATH)).toAbsolutePath();
    }

    /** Reads and caches the external JSON file. Returns empty defaults if it is missing or invalid. */
    public static synchronized SeedData load() {
        if (cached != null) return cached;
        Path file = dataFile();
        if (!Files.exists(file)) {
            System.err.println("[DealerLink] JSON data file not found at " + file + " - using built-in defaults.");
            cached = new SeedData();
            return cached;
        }
        try {
            cached = MAPPER.readValue(file.toFile(), SeedData.class);
            System.out.println("[DealerLink] Loaded " + cached.getUsers().size() + " users, "
                    + cached.getProducts().size() + " products, "
                    + cached.getInventory().size() + " inventory rows from " + file);
        } catch (IOException e) {
            System.err.println("[DealerLink] Could not parse " + file + ": " + e.getMessage());
            cached = new SeedData();
        }
        return cached;
    }

    /** Shortcut for the "settings" block. */
    public static AppSettings settings() {
        return load().getSettings();
    }

    /** Writes any object/list to a pretty-printed JSON file (used by "Export JSON" buttons). */
    public static void exportToFile(Object data, File target) throws IOException {
        MAPPER.writeValue(target, data);
    }
}
