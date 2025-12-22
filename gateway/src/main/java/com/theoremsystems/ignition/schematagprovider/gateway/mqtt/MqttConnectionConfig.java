package com.theoremsystems.ignition.schematagprovider.gateway.mqtt;

import com.theoremsystems.ignition.schematagprovider.gateway.config.ModuleSettings;

/**
 * Configuration for MQTT broker connection.
 */
public class MqttConnectionConfig {

    private String brokerUrl;
    private String clientId;
    private String topic;
    private String username;
    private String password;
    private int qos;
    private boolean cleanSession;
    private int connectionTimeout;
    private int keepAliveInterval;
    private boolean automaticReconnect;

    public MqttConnectionConfig() {
        // Set defaults
        this.brokerUrl = "tcp://localhost:1883";
        this.clientId = "ignition-schema-provider";
        this.topic = "ignition/schemas/#";
        this.qos = 1;
        this.cleanSession = true;
        this.connectionTimeout = 30;
        this.keepAliveInterval = 60;
        this.automaticReconnect = true;
    }

    /**
     * Create config from ModuleSettings.
     */
    public static MqttConnectionConfig fromSettings(ModuleSettings settings) {
        MqttConnectionConfig config = new MqttConnectionConfig();
        config.setBrokerUrl(settings.getMqttBrokerUrl());
        config.setClientId(settings.getMqttClientId());
        config.setTopic(settings.getMqttTopic());
        config.setUsername(settings.getMqttUsername());
        config.setPassword(settings.getMqttPassword());
        config.setQos(settings.getMqttQos());
        return config;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public boolean isAutomaticReconnect() {
        return automaticReconnect;
    }

    public void setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

    public boolean hasCredentials() {
        return username != null && !username.isEmpty();
    }

    @Override
    public String toString() {
        return "MqttConnectionConfig{" +
                "brokerUrl='" + brokerUrl + '\'' +
                ", clientId='" + clientId + '\'' +
                ", topic='" + topic + '\'' +
                ", qos=" + qos +
                ", automaticReconnect=" + automaticReconnect +
                '}';
    }
}
