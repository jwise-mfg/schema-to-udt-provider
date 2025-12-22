package com.theoremsystems.ignition.schematagprovider.gateway;

import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.theoremsystems.ignition.schematagprovider.gateway.config.ModuleSettings;
import com.theoremsystems.ignition.schematagprovider.gateway.mqtt.MqttConnectionConfig;
import com.theoremsystems.ignition.schematagprovider.gateway.mqtt.MqttSchemaListener;
import com.theoremsystems.ignition.schematagprovider.gateway.mqtt.SchemaMessageHandler;
import com.theoremsystems.ignition.schematagprovider.gateway.schema.JsonSchemaParser;
import com.theoremsystems.ignition.schematagprovider.gateway.schema.SchemaCacheManager;
import com.theoremsystems.ignition.schematagprovider.gateway.schema.SchemaModel;
import com.theoremsystems.ignition.schematagprovider.gateway.udt.UdtSynchronizer;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Central coordinator for the Tag Provider module.
 * Manages the lifecycle of cache, UDT synchronizer, and MQTT listener.
 */
public class TagProviderManager implements SchemaMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(TagProviderManager.class);

    private final GatewayContext context;
    private final ModuleSettings settings;

    private SchemaCacheManager cacheManager;
    private UdtSynchronizer synchronizer;
    private MqttSchemaListener mqttListener;
    private ScheduledFuture<?> scanTask;

    private volatile boolean running = false;

    public TagProviderManager(GatewayContext context, ModuleSettings settings) {
        this.context = context;
        this.settings = settings;
    }

    /**
     * Start the tag provider manager.
     * Initializes cache, syncs existing schemas, and starts MQTT listener.
     */
    public void startup() {
        logger.info("Starting TagProviderManager with settings: {}", settings);

        try {
            // 1. Initialize the schema cache
            initializeCache();

            // 2. Create UDT synchronizer
            synchronizer = new UdtSynchronizer(context, settings.getTagProviderName());

            // 3. Start MQTT listener if enabled (do this before sync so we don't miss updates)
            if (settings.isMqttEnabled()) {
                startMqttListener();
            } else {
                logger.info("MQTT listener disabled by configuration");
            }

            running = true;

            // 4. Sync all cached schemas to UDT definitions
            // This is done after setting running=true so the module is considered started
            // even if UDT sync fails (tag provider might not be ready yet)
            try {
                syncAllSchemas();
            } catch (Exception e) {
                logger.warn("Failed to sync schemas on startup. Will retry when schemas are received via MQTT. Error: {}", e.getMessage());
            }

            // 5. Start periodic cache scan
            startCacheScanTask();

            logger.info("TagProviderManager started successfully");

        } catch (Exception e) {
            logger.error("Failed to start TagProviderManager", e);
            // Don't throw - allow module to start even if there are issues
            // The module can still receive MQTT messages and retry later
            running = true;
        }
    }

    /**
     * Shutdown the tag provider manager.
     */
    public void shutdown() {
        logger.info("Shutting down TagProviderManager");
        running = false;

        // Stop periodic scan task
        if (scanTask != null) {
            scanTask.cancel(false);
            scanTask = null;
        }

        // Stop MQTT listener
        if (mqttListener != null) {
            mqttListener.disconnect();
            mqttListener = null;
        }

        logger.info("TagProviderManager shutdown complete");
    }

    private void initializeCache() throws IOException {
        // Resolve cache path relative to Ignition install directory
        Path cachePath = resolvePath(settings.getSchemaCachePath());
        logger.info("Initializing schema cache at: {}", cachePath);

        cacheManager = new SchemaCacheManager(cachePath);
        cacheManager.initialize();

        logger.info("Schema cache initialized with {} schemas", cacheManager.getSchemaCount());
    }

    private void syncAllSchemas() {
        logger.info("Syncing {} cached schemas to UDT definitions", cacheManager.getSchemaCount());

        int synced = synchronizer.syncAllUdtDefinitions(cacheManager.getAllSchemas());
        logger.info("Successfully synced {} UDT definitions", synced);
    }

    private void startMqttListener() {
        logger.info("Starting MQTT listener");

        MqttConnectionConfig mqttConfig = MqttConnectionConfig.fromSettings(settings);
        mqttListener = new MqttSchemaListener(mqttConfig, this);

        try {
            mqttListener.connect();
            logger.info("MQTT listener started successfully");
        } catch (MqttException e) {
            logger.error("Failed to connect to MQTT broker: {}. Schema updates via MQTT will not be available.",
                    mqttConfig.getBrokerUrl(), e);
            // Don't fail startup - module can still work with cached schemas
        }
    }

    private void startCacheScanTask() {
        int intervalSeconds = settings.getCacheScanIntervalSeconds();
        if (intervalSeconds <= 0) {
            logger.info("Cache scan disabled (interval: {} seconds)", intervalSeconds);
            return;
        }

        logger.info("Starting periodic cache scan task (interval: {} seconds)", intervalSeconds);

        scanTask = context.getExecutionManager().scheduleWithFixedDelay(
                this::scanAndSyncCache,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    /**
     * Scan the cache directory for new/updated schemas and sync them.
     * Also removes UDTs for schemas that were deleted from the cache.
     */
    private void scanAndSyncCache() {
        if (!running) {
            return;
        }

        try {
            int previousCount = cacheManager.getSchemaCount();
            Set<String> deletedSchemas = cacheManager.reload();
            int newCount = cacheManager.getSchemaCount();

            // Remove UDTs for deleted schemas (if allowed)
            if (settings.isAllowDelete()) {
                for (String schemaName : deletedSchemas) {
                    logger.info("Removing UDT for deleted schema: {}", schemaName);
                    synchronizer.removeUdtDefinition(schemaName);
                }
            } else if (!deletedSchemas.isEmpty()) {
                logger.info("Skipping UDT removal for {} deleted schemas (allowDelete=false)", deletedSchemas.size());
            }

            // Sync remaining schemas if count changed (new schemas added)
            if (newCount != previousCount || !deletedSchemas.isEmpty()) {
                logger.info("Cache scan detected changes ({} -> {} schemas, {} deleted), syncing UDTs",
                        previousCount, newCount, deletedSchemas.size());
                syncAllSchemas();
            } else {
                logger.debug("Cache scan complete, no changes detected ({} schemas)", newCount);
            }
        } catch (Exception e) {
            logger.error("Error during cache scan", e);
        }
    }

    private Path resolvePath(String pathString) {
        Path path = Paths.get(pathString);
        if (path.isAbsolute()) {
            return path;
        }
        // Resolve relative paths from Ignition data directory
        try {
            java.io.File dataDir = context.getSystemManager().getDataDir();
            return dataDir.toPath().resolve(pathString);
        } catch (Exception e) {
            logger.warn("Could not get data directory, using relative path: {}", e.getMessage());
            return path;
        }
    }

    // SchemaMessageHandler implementation

    @Override
    public void onSchemaReceived(String schemaName, String jsonSchemaContent) {
        if (!running) {
            logger.warn("Received schema while not running, ignoring: {}", schemaName);
            return;
        }

        logger.info("Processing received schema: {}", schemaName);

        try {
            // Save to cache (this also parses and validates)
            SchemaModel schema = cacheManager.saveSchema(schemaName, jsonSchemaContent);

            // Sync to UDT definition
            boolean success = synchronizer.syncUdtDefinition(schema);

            if (success) {
                logger.info("Successfully processed schema: {}", schemaName);
            } else {
                logger.error("Failed to sync schema to UDT: {}", schemaName);
            }

        } catch (IOException e) {
            logger.error("Failed to save schema to cache: " + schemaName, e);
        } catch (JsonSchemaParser.JsonSchemaParseException e) {
            logger.error("Invalid JSON Schema received: " + schemaName, e);
        }
    }

    @Override
    public void onSchemaDeleted(String schemaName) {
        if (!running) {
            return;
        }

        logger.info("Processing schema deletion: {}", schemaName);

        try {
            // Remove from UDT definitions (if allowed)
            if (settings.isAllowDelete()) {
                synchronizer.removeUdtDefinition(schemaName);
                logger.info("Removed UDT for schema: {}", schemaName);
            } else {
                logger.info("Skipping UDT removal for schema: {} (allowDelete=false)", schemaName);
            }

            // Always remove from cache (the file deletion message came from MQTT)
            cacheManager.removeSchema(schemaName);

            logger.info("Successfully processed schema deletion: {}", schemaName);

        } catch (IOException e) {
            logger.error("Failed to delete schema from cache: " + schemaName, e);
        }
    }

    @Override
    public void onConnected() {
        logger.info("MQTT connection established");
    }

    @Override
    public void onDisconnected(Throwable cause) {
        logger.warn("MQTT connection lost, will attempt reconnect", cause);
    }

    // Accessors for testing and monitoring

    public boolean isRunning() {
        return running;
    }

    public boolean isMqttConnected() {
        return mqttListener != null && mqttListener.isConnected();
    }

    public int getCachedSchemaCount() {
        return cacheManager != null ? cacheManager.getSchemaCount() : 0;
    }

    public int getRegisteredUdtCount() {
        return synchronizer != null ? synchronizer.getRegisteredTypes().size() : 0;
    }
}
