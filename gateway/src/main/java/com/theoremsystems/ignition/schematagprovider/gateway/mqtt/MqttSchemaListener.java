package com.theoremsystems.ignition.schematagprovider.gateway.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * MQTT client that listens for JSON Schema messages and forwards them to a handler.
 */
public class MqttSchemaListener implements MqttCallback {

    private static final Logger logger = LoggerFactory.getLogger(MqttSchemaListener.class);

    private final MqttConnectionConfig config;
    private final SchemaMessageHandler handler;

    private MqttClient client;
    private volatile boolean connected = false;

    public MqttSchemaListener(MqttConnectionConfig config, SchemaMessageHandler handler) {
        this.config = config;
        this.handler = handler;
    }

    /**
     * Connect to the MQTT broker and subscribe to the schema topic.
     */
    public void connect() throws MqttException {
        logger.info("Connecting to MQTT broker: {}", config.getBrokerUrl());

        // Create client with memory persistence
        client = new MqttClient(
                config.getBrokerUrl(),
                config.getClientId(),
                new MemoryPersistence()
        );

        // Configure connection options
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(config.isCleanSession());
        options.setAutomaticReconnect(config.isAutomaticReconnect());
        options.setConnectionTimeout(config.getConnectionTimeout());
        options.setKeepAliveInterval(config.getKeepAliveInterval());

        // Set credentials if provided
        if (config.hasCredentials()) {
            options.setUserName(config.getUsername());
            options.setPassword(config.getPassword().toCharArray());
        }

        // Set callback before connecting
        client.setCallback(this);

        // Connect
        client.connect(options);
        connected = true;
        logger.info("Connected to MQTT broker");

        // Subscribe to topic
        client.subscribe(config.getTopic(), config.getQos());
        logger.info("Subscribed to topic: {} with QoS {}", config.getTopic(), config.getQos());

        handler.onConnected();
    }

    /**
     * Disconnect from the MQTT broker.
     */
    public void disconnect() {
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.unsubscribe(config.getTopic());
                    client.disconnect();
                    logger.info("Disconnected from MQTT broker");
                }
                client.close();
            } catch (MqttException e) {
                logger.error("Error disconnecting from MQTT broker", e);
            }
            connected = false;
        }
    }

    /**
     * Check if connected to the broker.
     */
    public boolean isConnected() {
        return connected && client != null && client.isConnected();
    }

    // MqttCallback implementation

    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("MQTT connection lost", cause);
        connected = false;
        handler.onDisconnected(cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

            // Extract schema name from topic
            String schemaName = extractSchemaName(topic);

            if (schemaName == null || schemaName.isEmpty()) {
                logger.warn("Could not extract schema name from topic: {}", topic);
                return;
            }

            // Empty payload means delete
            if (payload.isEmpty() || payload.trim().isEmpty()) {
                logger.info("Received delete signal for schema: {}", schemaName);
                handler.onSchemaDeleted(schemaName);
            } else {
                logger.info("Received schema update: {} ({} bytes)", schemaName, payload.length());
                logger.debug("Schema content: {}", payload);
                handler.onSchemaReceived(schemaName, payload);
            }
        } catch (Exception e) {
            logger.error("Error processing MQTT message from topic: " + topic, e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Not used for subscribing
    }

    /**
     * Extract the schema name from the MQTT topic.
     * <p>
     * For example, if the base topic is "ignition/schemas/#":
     * - "ignition/schemas/Sensor" -> "Sensor"
     * - "ignition/schemas/devices/Temperature" -> "devices_Temperature"
     */
    private String extractSchemaName(String topic) {
        // Get the base topic without wildcards
        String baseTopic = config.getTopic()
                .replace("#", "")
                .replace("+", "");

        // Remove trailing slash if present
        if (baseTopic.endsWith("/")) {
            baseTopic = baseTopic.substring(0, baseTopic.length() - 1);
        }

        // Extract the part after the base topic
        if (topic.startsWith(baseTopic)) {
            String suffix = topic.substring(baseTopic.length());
            if (suffix.startsWith("/")) {
                suffix = suffix.substring(1);
            }
            // Replace any remaining slashes with underscores for the schema name
            return suffix.replace("/", "_");
        }

        // Fallback: use the last segment of the topic
        String[] parts = topic.split("/");
        return parts[parts.length - 1];
    }
}
