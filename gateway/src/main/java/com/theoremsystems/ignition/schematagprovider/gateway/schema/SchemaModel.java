package com.theoremsystems.ignition.schematagprovider.gateway.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Internal representation of a parsed JSON Schema.
 * This model is used to generate Ignition UDT definitions.
 */
public class SchemaModel {

    private String name;              // Schema/UDT name (from title or filename)
    private String id;                // JSON Schema $id
    private String description;
    private String parentType;        // For inheritance (from $ref to base type)

    private List<PropertyDefinition> properties = new ArrayList<>();
    private Set<String> required = new HashSet<>();

    public SchemaModel() {
    }

    public SchemaModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }

    public List<PropertyDefinition> getProperties() {
        return properties;
    }

    public void setProperties(List<PropertyDefinition> properties) {
        this.properties = properties;
    }

    public void addProperty(PropertyDefinition property) {
        this.properties.add(property);
    }

    public Set<String> getRequired() {
        return required;
    }

    public void setRequired(Set<String> required) {
        this.required = required;
    }

    public void addRequired(String propertyName) {
        this.required.add(propertyName);
    }

    public boolean isRequired(String propertyName) {
        return required.contains(propertyName);
    }

    public boolean hasParent() {
        return parentType != null && !parentType.isEmpty();
    }

    @Override
    public String toString() {
        return "SchemaModel{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", properties=" + properties.size() +
                ", required=" + required +
                '}';
    }
}
