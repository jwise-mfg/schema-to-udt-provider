package com.theoremsystems.ignition.schematagprovider.gateway.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single property/field from a JSON Schema.
 */
public class PropertyDefinition {

    private String name;
    private String type;           // string, integer, number, boolean, object, array
    private String format;         // date-time, email, uri, etc.
    private String description;
    private Object defaultValue;
    private String refType;        // For $ref references to other schemas

    // For nested objects
    private List<PropertyDefinition> nestedProperties = new ArrayList<>();

    // For arrays
    private PropertyDefinition itemsDefinition;

    // For enums
    private List<String> enumValues;

    public PropertyDefinition() {
    }

    public PropertyDefinition(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
    }

    public List<PropertyDefinition> getNestedProperties() {
        return nestedProperties;
    }

    public void setNestedProperties(List<PropertyDefinition> nestedProperties) {
        this.nestedProperties = nestedProperties;
    }

    public void addNestedProperty(PropertyDefinition property) {
        this.nestedProperties.add(property);
    }

    public PropertyDefinition getItemsDefinition() {
        return itemsDefinition;
    }

    public void setItemsDefinition(PropertyDefinition itemsDefinition) {
        this.itemsDefinition = itemsDefinition;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }

    public boolean isObject() {
        return "object".equals(type);
    }

    public boolean isArray() {
        return "array".equals(type);
    }

    public boolean isReference() {
        return refType != null && !refType.isEmpty();
    }

    public boolean hasNestedProperties() {
        return nestedProperties != null && !nestedProperties.isEmpty();
    }

    public boolean hasEnum() {
        return enumValues != null && !enumValues.isEmpty();
    }

    @Override
    public String toString() {
        return "PropertyDefinition{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", format='" + format + '\'' +
                ", refType='" + refType + '\'' +
                '}';
    }
}
