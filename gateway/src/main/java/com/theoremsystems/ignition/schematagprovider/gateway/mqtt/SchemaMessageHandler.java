package com.theoremsystems.ignition.schematagprovider.gateway.mqtt;

/**
 * Callback interface for handling schema messages received via MQTT.
 */
public interface SchemaMessageHandler {

    /**
     * Called when a new or updated schema is received.
     *
     * @param schemaName        The name of the schema (derived from topic)
     * @param jsonSchemaContent The JSON Schema content
     */
    void onSchemaReceived(String schemaName, String jsonSchemaContent);

    /**
     * Called when a schema deletion is signaled (empty payload or delete topic).
     *
     * @param schemaName The name of the schema to delete
     */
    void onSchemaDeleted(String schemaName);

    /**
     * Called when MQTT connection is established.
     */
    default void onConnected() {
    }

    /**
     * Called when MQTT connection is lost.
     *
     * @param cause The cause of disconnection
     */
    default void onDisconnected(Throwable cause) {
    }
}
