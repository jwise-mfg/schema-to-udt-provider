package com.theoremsystems.ignition.schematagprovider.gateway.udt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.theoremsystems.ignition.schematagprovider.gateway.schema.DataTypeMapper;
import com.theoremsystems.ignition.schematagprovider.gateway.schema.PropertyDefinition;
import com.theoremsystems.ignition.schematagprovider.gateway.schema.SchemaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds Ignition UDT definition JSON from SchemaModel objects.
 */
public class UdtDefinitionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(UdtDefinitionBuilder.class);

    private final Gson gson;

    public UdtDefinitionBuilder() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Build the UDT definition JSON for a schema.
     *
     * @param schema The schema to convert
     * @return JSON string suitable for import into Ignition
     */
    public String buildUdtJson(SchemaModel schema) {
        JsonObject udt = new JsonObject();

        // Set basic properties
        udt.addProperty("name", schema.getName());
        udt.addProperty("tagType", "UdtType");

        // Add description as documentation if present
        if (schema.getDescription() != null && !schema.getDescription().isEmpty()) {
            udt.addProperty("documentation", schema.getDescription());
        }

        // Handle inheritance (parent type)
        if (schema.hasParent()) {
            udt.addProperty("typeId", schema.getParentType());
        }

        // Build member tags
        JsonArray tags = new JsonArray();
        for (PropertyDefinition prop : schema.getProperties()) {
            JsonObject tag = buildTagDefinition(prop, schema);
            if (tag != null) {
                tags.add(tag);
            }
        }
        udt.add("tags", tags);

        String json = gson.toJson(udt);
        logger.debug("Built UDT JSON for {}: {}", schema.getName(), json);
        return json;
    }

    /**
     * Build a JSON array containing multiple UDT definitions.
     * Useful for batch import.
     */
    public String buildUdtJsonArray(Iterable<SchemaModel> schemas) {
        JsonArray array = new JsonArray();
        for (SchemaModel schema : schemas) {
            JsonObject udt = gson.fromJson(buildUdtJson(schema), JsonObject.class);
            array.add(udt);
        }
        return gson.toJson(array);
    }

    private JsonObject buildTagDefinition(PropertyDefinition prop, SchemaModel parentSchema) {
        JsonObject tag = new JsonObject();
        tag.addProperty("name", prop.getName());

        // Add documentation/tooltip if description present
        if (prop.getDescription() != null && !prop.getDescription().isEmpty()) {
            tag.addProperty("tooltip", prop.getDescription());
        }

        // Handle reference to another UDT
        if (prop.isReference()) {
            tag.addProperty("tagType", "UdtInstance");
            tag.addProperty("typeId", prop.getRefType());
            return tag;
        }

        // Handle nested object (becomes nested UDT instance)
        if (prop.isObject() && prop.hasNestedProperties()) {
            // Create a nested UDT type name based on parent and property name
            String nestedTypeName = parentSchema.getName() + "_" + prop.getName();
            tag.addProperty("tagType", "UdtInstance");
            tag.addProperty("typeId", nestedTypeName);
            // Note: The nested UDT definition would need to be created separately
            return tag;
        }

        // Handle array type
        if (prop.isArray()) {
            tag.addProperty("tagType", "AtomicTag");
            tag.addProperty("valueSource", "memory");
            tag.addProperty("dataType", "DataSet");
            addReadOnlyConfig(tag);
            return tag;
        }

        // Handle primitive types
        String ignitionType = DataTypeMapper.mapToIgnitionType(prop.getType(), prop.getFormat());
        if (ignitionType == null) {
            logger.warn("Could not map type for property: {} type: {}", prop.getName(), prop.getType());
            return null;
        }

        tag.addProperty("tagType", "AtomicTag");
        tag.addProperty("valueSource", "memory");
        tag.addProperty("dataType", ignitionType);

        // Add default value if present
        if (prop.getDefaultValue() != null) {
            addDefaultValue(tag, prop);
        }

        // Configure as read-only
        addReadOnlyConfig(tag);

        return tag;
    }

    private void addReadOnlyConfig(JsonObject tag) {
        // Set read-only access rights
        JsonObject accessRights = new JsonObject();
        accessRights.addProperty("accessRights", "Read_Only");
        tag.add("readPermissions", accessRights);
    }

    private void addDefaultValue(JsonObject tag, PropertyDefinition prop) {
        Object defaultVal = prop.getDefaultValue();
        if (defaultVal instanceof Boolean) {
            tag.addProperty("value", (Boolean) defaultVal);
        } else if (defaultVal instanceof Number) {
            tag.addProperty("value", (Number) defaultVal);
        } else {
            tag.addProperty("value", defaultVal.toString());
        }
    }

    /**
     * Build nested UDT definitions for schemas that have nested objects.
     * These need to be created before the parent UDT.
     *
     * @param schema The parent schema
     * @return JSON array of nested UDT definitions, or null if none
     */
    public String buildNestedUdtDefinitions(SchemaModel schema) {
        JsonArray nestedUdts = new JsonArray();

        for (PropertyDefinition prop : schema.getProperties()) {
            if (prop.isObject() && prop.hasNestedProperties() && !prop.isReference()) {
                // Create a synthetic schema for the nested object
                SchemaModel nestedSchema = new SchemaModel();
                nestedSchema.setName(schema.getName() + "_" + prop.getName());
                nestedSchema.setDescription("Nested type for " + schema.getName() + "." + prop.getName());
                nestedSchema.setProperties(prop.getNestedProperties());

                JsonObject nestedUdt = gson.fromJson(buildUdtJson(nestedSchema), JsonObject.class);
                nestedUdts.add(nestedUdt);

                // Recursively handle deeply nested objects
                String deepNested = buildNestedUdtDefinitions(nestedSchema);
                if (deepNested != null) {
                    JsonArray deepArray = gson.fromJson(deepNested, JsonArray.class);
                    deepArray.forEach(nestedUdts::add);
                }
            }
        }

        if (nestedUdts.size() == 0) {
            return null;
        }

        return gson.toJson(nestedUdts);
    }
}
