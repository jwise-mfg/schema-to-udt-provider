package com.theoremsystems.ignition.schematagprovider.gateway.udt;

import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import com.theoremsystems.ignition.schematagprovider.gateway.schema.SchemaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Synchronizes UDT definitions with Ignition's TagProvider.
 * Handles importing UDT definitions to the _types_ folder.
 */
public class UdtSynchronizer {

    private static final Logger logger = LoggerFactory.getLogger(UdtSynchronizer.class);

    private static final String TYPES_PATH = "_types_";
    private static final long IMPORT_TIMEOUT_SECONDS = 30;

    private final GatewayContext context;
    private final UdtDefinitionBuilder builder;
    private final String providerName;
    private final Set<String> registeredTypes = new HashSet<>();

    public UdtSynchronizer(GatewayContext context, String providerName) {
        this.context = context;
        this.providerName = providerName;
        this.builder = new UdtDefinitionBuilder();
    }

    /**
     * Synchronize a schema to an Ignition UDT definition.
     *
     * @param schema The schema to sync
     * @return true if successful, false otherwise
     */
    public boolean syncUdtDefinition(SchemaModel schema) {
        logger.info("Syncing UDT definition: {}", schema.getName());

        try {
            TagProvider provider = getTagProvider();
            if (provider == null) {
                logger.error("Tag provider not found: {}", providerName);
                return false;
            }

            // First, import any nested UDT definitions
            String nestedJson = builder.buildNestedUdtDefinitions(schema);
            if (nestedJson != null) {
                logger.debug("Importing nested UDT definitions for: {}", schema.getName());
                boolean nestedSuccess = importUdtJson(provider, nestedJson);
                if (!nestedSuccess) {
                    logger.warn("Failed to import nested UDTs for: {}", schema.getName());
                }
            }

            // Build and import the main UDT definition
            String udtJson = builder.buildUdtJson(schema);
            boolean success = importUdtJson(provider, udtJson);

            if (success) {
                registeredTypes.add(schema.getName());
                logger.info("Successfully synced UDT: {}", schema.getName());
            } else {
                logger.error("Failed to sync UDT: {}", schema.getName());
            }

            return success;

        } catch (Exception e) {
            logger.error("Error syncing UDT definition: " + schema.getName(), e);
            return false;
        }
    }

    /**
     * Synchronize multiple schemas.
     *
     * @param schemas The schemas to sync
     * @return Number of successfully synced schemas
     */
    public int syncAllUdtDefinitions(Iterable<SchemaModel> schemas) {
        int successCount = 0;
        for (SchemaModel schema : schemas) {
            if (syncUdtDefinition(schema)) {
                successCount++;
            }
        }
        logger.info("Synced {}/{} UDT definitions", successCount, registeredTypes.size());
        return successCount;
    }

    /**
     * Remove a UDT definition from Ignition.
     *
     * @param schemaName The name of the UDT to remove
     * @return true if successful
     */
    public boolean removeUdtDefinition(String schemaName) {
        logger.info("Removing UDT definition: {}", schemaName);

        try {
            TagProvider provider = getTagProvider();
            if (provider == null) {
                return false;
            }

            // Remove the tag at _types_/schemaName
            var tagPath = TagPathParser.parse(TYPES_PATH + "/" + schemaName);
            CompletableFuture<List<QualityCode>> future = provider.removeTagConfigsAsync(List.of(tagPath));

            List<QualityCode> results = future.get(IMPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            boolean success = results.stream().allMatch(QualityCode::isGood);
            if (success) {
                registeredTypes.remove(schemaName);
                logger.info("Removed UDT: {}", schemaName);
            } else {
                logger.error("Failed to remove UDT: {} - {}", schemaName, results);
            }

            return success;

        } catch (Exception e) {
            logger.error("Error removing UDT: " + schemaName, e);
            return false;
        }
    }

    private boolean importUdtJson(TagProvider provider, String json) {
        try {
            var typesPath = TagPathParser.parse(TYPES_PATH);

            // Import using Overwrite collision policy to update existing UDTs
            CompletableFuture<List<QualityCode>> future = provider.importTagsAsync(
                    typesPath,
                    json,
                    "json",
                    com.inductiveautomation.ignition.common.tags.config.CollisionPolicy.Overwrite
            );

            List<QualityCode> results = future.get(IMPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            boolean success = results.stream().allMatch(QualityCode::isGood);
            if (!success) {
                for (int i = 0; i < results.size(); i++) {
                    if (!results.get(i).isGood()) {
                        logger.error("Import error at index {}: {}", i, results.get(i));
                    }
                }
            }

            return success;

        } catch (Exception e) {
            logger.error("Error importing UDT JSON", e);
            return false;
        }
    }

    private TagProvider getTagProvider() {
        try {
            GatewayTagManager tagManager = context.getTagManager();
            if (tagManager == null) {
                logger.error("Tag manager not available yet");
                return null;
            }

            TagProvider provider = tagManager.getTagProvider(providerName);

            if (provider == null) {
                try {
                    logger.error("Tag provider '{}' not found. Available providers: {}",
                            providerName, tagManager.getTagProviderNames());
                } catch (Exception e) {
                    logger.error("Tag provider '{}' not found, and could not list available providers", providerName);
                }
            }

            return provider;
        } catch (Exception e) {
            logger.error("Error getting tag provider: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the set of registered UDT type names.
     */
    public Set<String> getRegisteredTypes() {
        return new HashSet<>(registeredTypes);
    }

    /**
     * Check if a UDT type is registered.
     */
    public boolean isTypeRegistered(String typeName) {
        return registeredTypes.contains(typeName);
    }
}
