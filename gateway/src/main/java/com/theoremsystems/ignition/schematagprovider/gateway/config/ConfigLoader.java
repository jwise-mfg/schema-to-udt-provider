package com.theoremsystems.ignition.schematagprovider.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads and saves module configuration from a properties file.
 */
public class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String CONFIG_FILENAME = "config.properties";

    // Property keys
    private static final String MQTT_BROKER_URL = "mqtt.broker.url";
    private static final String MQTT_CLIENT_ID = "mqtt.client.id";
    private static final String MQTT_TOPIC = "mqtt.topic";
    private static final String MQTT_USERNAME = "mqtt.username";
    private static final String MQTT_PASSWORD = "mqtt.password";
    private static final String MQTT_QOS = "mqtt.qos";
    private static final String MQTT_ENABLED = "mqtt.enabled";
    private static final String SCHEMA_CACHE_PATH = "schema.cache.path";
    private static final String CACHE_SCAN_INTERVAL = "schema.cache.scan.interval.seconds";
    private static final String TAG_PROVIDER_NAME = "tag.provider.name";
    private static final String ALLOW_DELETE = "tag.provider.allowdelete";

    private final Path configDirectory;

    public ConfigLoader(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    /**
     * Load settings from the config file. If the file doesn't exist,
     * create it with default values.
     */
    public ModuleSettings load() {
        Path configFile = configDirectory.resolve(CONFIG_FILENAME);
        ModuleSettings settings = new ModuleSettings();

        if (Files.exists(configFile)) {
            logger.info("Loading configuration from: {}", configFile);
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(configFile)) {
                props.load(is);
                applyProperties(props, settings);
            } catch (IOException e) {
                logger.error("Failed to load config file, using defaults", e);
            }
        } else {
            logger.info("Config file not found, creating with defaults: {}", configFile);
            save(settings);
        }

        logger.info("Module settings: {}", settings);
        return settings;
    }

    /**
     * Save settings to the config file.
     */
    public void save(ModuleSettings settings) {
        Path configFile = configDirectory.resolve(CONFIG_FILENAME);

        try {
            Files.createDirectories(configDirectory);

            Properties props = new Properties();
            props.setProperty(MQTT_BROKER_URL, settings.getMqttBrokerUrl());
            props.setProperty(MQTT_CLIENT_ID, settings.getMqttClientId());
            props.setProperty(MQTT_TOPIC, settings.getMqttTopic());
            props.setProperty(MQTT_USERNAME, settings.getMqttUsername());
            props.setProperty(MQTT_PASSWORD, settings.getMqttPassword());
            props.setProperty(MQTT_QOS, String.valueOf(settings.getMqttQos()));
            props.setProperty(MQTT_ENABLED, String.valueOf(settings.isMqttEnabled()));
            props.setProperty(SCHEMA_CACHE_PATH, settings.getSchemaCachePath());
            props.setProperty(CACHE_SCAN_INTERVAL, String.valueOf(settings.getCacheScanIntervalSeconds()));
            props.setProperty(TAG_PROVIDER_NAME, settings.getTagProviderName());
            props.setProperty(ALLOW_DELETE, String.valueOf(settings.isAllowDelete()));

            try (OutputStream os = Files.newOutputStream(configFile)) {
                props.store(os, "Schema Tag Provider Module Configuration");
            }

            logger.info("Configuration saved to: {}", configFile);
        } catch (IOException e) {
            logger.error("Failed to save config file", e);
        }
    }

    private void applyProperties(Properties props, ModuleSettings settings) {
        if (props.containsKey(MQTT_BROKER_URL)) {
            settings.setMqttBrokerUrl(props.getProperty(MQTT_BROKER_URL));
        }
        if (props.containsKey(MQTT_CLIENT_ID)) {
            settings.setMqttClientId(props.getProperty(MQTT_CLIENT_ID));
        }
        if (props.containsKey(MQTT_TOPIC)) {
            settings.setMqttTopic(props.getProperty(MQTT_TOPIC));
        }
        if (props.containsKey(MQTT_USERNAME)) {
            settings.setMqttUsername(props.getProperty(MQTT_USERNAME));
        }
        if (props.containsKey(MQTT_PASSWORD)) {
            settings.setMqttPassword(props.getProperty(MQTT_PASSWORD));
        }
        if (props.containsKey(MQTT_QOS)) {
            try {
                settings.setMqttQos(Integer.parseInt(props.getProperty(MQTT_QOS)));
            } catch (NumberFormatException e) {
                logger.warn("Invalid mqtt.qos value, using default");
            }
        }
        if (props.containsKey(MQTT_ENABLED)) {
            settings.setMqttEnabled(Boolean.parseBoolean(props.getProperty(MQTT_ENABLED)));
        }
        if (props.containsKey(SCHEMA_CACHE_PATH)) {
            settings.setSchemaCachePath(props.getProperty(SCHEMA_CACHE_PATH));
        }
        if (props.containsKey(CACHE_SCAN_INTERVAL)) {
            try {
                settings.setCacheScanIntervalSeconds(Integer.parseInt(props.getProperty(CACHE_SCAN_INTERVAL)));
            } catch (NumberFormatException e) {
                logger.warn("Invalid {} value, using default", CACHE_SCAN_INTERVAL);
            }
        }
        if (props.containsKey(TAG_PROVIDER_NAME)) {
            settings.setTagProviderName(props.getProperty(TAG_PROVIDER_NAME));
        }
        if (props.containsKey(ALLOW_DELETE)) {
            settings.setAllowDelete(Boolean.parseBoolean(props.getProperty(ALLOW_DELETE)));
        }
    }
}
