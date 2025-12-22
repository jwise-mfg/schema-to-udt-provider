package com.theoremsystems.ignition.schematagprovider.gateway.config;

/**
 * Configuration settings for the Tag Provider module.
 * Loaded from a properties file at startup.
 */
public class ModuleSettings {

    // MQTT Configuration
    private String mqttBrokerUrl = "tcp://localhost:1883";
    private String mqttClientId = "ignition-schema-provider";
    private String mqttTopic = "ignition/schemas/#";
    private String mqttUsername = "";
    private String mqttPassword = "";
    private int mqttQos = 1;
    private boolean mqttEnabled = true;

    // Cache Configuration (relative to Ignition data directory)
    private String schemaCachePath = "modules/schema-tag-provider/schemas";
    private int cacheScanIntervalSeconds = 30;

    // Tag Provider Configuration
    private String tagProviderName = "default";
    private boolean allowDelete = true;

    public String getMqttBrokerUrl() {
        return mqttBrokerUrl;
    }

    public void setMqttBrokerUrl(String mqttBrokerUrl) {
        this.mqttBrokerUrl = mqttBrokerUrl;
    }

    public String getMqttClientId() {
        return mqttClientId;
    }

    public void setMqttClientId(String mqttClientId) {
        this.mqttClientId = mqttClientId;
    }

    public String getMqttTopic() {
        return mqttTopic;
    }

    public void setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    public String getMqttUsername() {
        return mqttUsername;
    }

    public void setMqttUsername(String mqttUsername) {
        this.mqttUsername = mqttUsername;
    }

    public String getMqttPassword() {
        return mqttPassword;
    }

    public void setMqttPassword(String mqttPassword) {
        this.mqttPassword = mqttPassword;
    }

    public int getMqttQos() {
        return mqttQos;
    }

    public void setMqttQos(int mqttQos) {
        this.mqttQos = mqttQos;
    }

    public boolean isMqttEnabled() {
        return mqttEnabled;
    }

    public void setMqttEnabled(boolean mqttEnabled) {
        this.mqttEnabled = mqttEnabled;
    }

    public String getSchemaCachePath() {
        return schemaCachePath;
    }

    public void setSchemaCachePath(String schemaCachePath) {
        this.schemaCachePath = schemaCachePath;
    }

    public int getCacheScanIntervalSeconds() {
        return cacheScanIntervalSeconds;
    }

    public void setCacheScanIntervalSeconds(int cacheScanIntervalSeconds) {
        this.cacheScanIntervalSeconds = cacheScanIntervalSeconds;
    }

    public String getTagProviderName() {
        return tagProviderName;
    }

    public void setTagProviderName(String tagProviderName) {
        this.tagProviderName = tagProviderName;
    }

    public boolean isAllowDelete() {
        return allowDelete;
    }

    public void setAllowDelete(boolean allowDelete) {
        this.allowDelete = allowDelete;
    }

    @Override
    public String toString() {
        return "ModuleSettings{" +
                "mqttBrokerUrl='" + mqttBrokerUrl + '\'' +
                ", mqttClientId='" + mqttClientId + '\'' +
                ", mqttTopic='" + mqttTopic + '\'' +
                ", mqttEnabled=" + mqttEnabled +
                ", schemaCachePath='" + schemaCachePath + '\'' +
                ", cacheScanIntervalSeconds=" + cacheScanIntervalSeconds +
                ", tagProviderName='" + tagProviderName + '\'' +
                ", allowDelete=" + allowDelete +
                '}';
    }
}
