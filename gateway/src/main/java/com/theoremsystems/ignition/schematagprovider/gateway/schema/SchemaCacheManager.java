package com.theoremsystems.ignition.schematagprovider.gateway.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a local file cache of JSON Schema files.
 * Provides thread-safe access to schemas and handles persistence.
 */
public class SchemaCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(SchemaCacheManager.class);

    private final Path cacheDirectory;
    private final JsonSchemaParser parser;
    private final Map<String, SchemaModel> schemaCache = new ConcurrentHashMap<>();
    private final Map<String, String> rawSchemaCache = new ConcurrentHashMap<>();

    public SchemaCacheManager(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        this.parser = new JsonSchemaParser();
    }

    /**
     * Initialize the cache manager: create directories and load existing schemas.
     */
    public void initialize() throws IOException {
        logger.info("Initializing schema cache at: {}", cacheDirectory);

        // Create cache directory if it doesn't exist
        Files.createDirectories(cacheDirectory);

        // Load all existing schemas
        loadAllSchemas();

        logger.info("Schema cache initialized with {} schemas", schemaCache.size());
    }

    /**
     * Load all JSON schema files from the cache directory.
     */
    private void loadAllSchemas() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDirectory, "*.json")) {
            for (Path file : stream) {
                try {
                    loadSchemaFile(file);
                } catch (Exception e) {
                    logger.error("Failed to load schema file: {}", file, e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read cache directory", e);
        }
    }

    private void loadSchemaFile(Path file) throws IOException, JsonSchemaParser.JsonSchemaParseException {
        String filename = file.getFileName().toString();
        String schemaName = filename.substring(0, filename.lastIndexOf('.'));
        String content = Files.readString(file);

        SchemaModel schema = parser.parse(schemaName, content);
        schemaCache.put(schemaName, schema);
        rawSchemaCache.put(schemaName, content);

        logger.debug("Loaded schema: {} from {}", schemaName, file);
    }

    /**
     * Save a new schema to the cache and persist to disk.
     *
     * @param schemaName The name for the schema
     * @param content    The raw JSON Schema content
     * @return The parsed SchemaModel
     */
    public SchemaModel saveSchema(String schemaName, String content) throws IOException, JsonSchemaParser.JsonSchemaParseException {
        // Parse first to validate
        SchemaModel schema = parser.parse(schemaName, content);

        // Save to disk
        Path file = cacheDirectory.resolve(schemaName + ".json");
        Files.writeString(file, content);

        // Update caches
        schemaCache.put(schemaName, schema);
        rawSchemaCache.put(schemaName, content);

        logger.info("Saved schema: {} to {}", schemaName, file);
        return schema;
    }

    /**
     * Remove a schema from the cache and delete from disk.
     */
    public void removeSchema(String schemaName) throws IOException {
        Path file = cacheDirectory.resolve(schemaName + ".json");

        if (Files.exists(file)) {
            Files.delete(file);
        }

        schemaCache.remove(schemaName);
        rawSchemaCache.remove(schemaName);

        logger.info("Removed schema: {}", schemaName);
    }

    /**
     * Get a schema by name.
     */
    public SchemaModel getSchema(String schemaName) {
        return schemaCache.get(schemaName);
    }

    /**
     * Get the raw JSON content for a schema.
     */
    public String getRawSchema(String schemaName) {
        return rawSchemaCache.get(schemaName);
    }

    /**
     * Get all cached schemas.
     */
    public Collection<SchemaModel> getAllSchemas() {
        return schemaCache.values();
    }

    /**
     * Get all schema names.
     */
    public Collection<String> getSchemaNames() {
        return schemaCache.keySet();
    }

    /**
     * Check if a schema exists in the cache.
     */
    public boolean hasSchema(String schemaName) {
        return schemaCache.containsKey(schemaName);
    }

    /**
     * Get the number of cached schemas.
     */
    public int getSchemaCount() {
        return schemaCache.size();
    }

    /**
     * Reload all schemas from disk. Useful if files were modified externally.
     *
     * @return Set of schema names that were deleted (present before reload but not after)
     */
    public Set<String> reload() {
        // Capture current schema names before clearing
        Set<String> previousSchemas = new HashSet<>(schemaCache.keySet());

        schemaCache.clear();
        rawSchemaCache.clear();
        loadAllSchemas();

        // Determine which schemas were deleted
        Set<String> deletedSchemas = new HashSet<>(previousSchemas);
        deletedSchemas.removeAll(schemaCache.keySet());

        if (!deletedSchemas.isEmpty()) {
            logger.info("Detected {} deleted schemas: {}", deletedSchemas.size(), deletedSchemas);
        }

        logger.info("Reloaded {} schemas from cache", schemaCache.size());
        return deletedSchemas;
    }

    /**
     * Get the cache directory path.
     */
    public Path getCacheDirectory() {
        return cacheDirectory;
    }
}
