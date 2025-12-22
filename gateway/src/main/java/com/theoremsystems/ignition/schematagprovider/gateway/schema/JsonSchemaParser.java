package com.theoremsystems.ignition.schematagprovider.gateway.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses JSON Schema files into SchemaModel objects.
 */
public class JsonSchemaParser {

    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaParser.class);

    /**
     * Parse a JSON Schema string into a SchemaModel.
     *
     * @param schemaName    The name to use for the schema (typically from filename)
     * @param jsonContent   The JSON Schema content as a string
     * @return The parsed SchemaModel
     * @throws JsonSchemaParseException if parsing fails
     */
    public SchemaModel parse(String schemaName, String jsonContent) throws JsonSchemaParseException {
        try {
            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            return parseSchema(schemaName, root);
        } catch (Exception e) {
            throw new JsonSchemaParseException("Failed to parse JSON Schema: " + schemaName, e);
        }
    }

    private SchemaModel parseSchema(String defaultName, JsonObject root) {
        SchemaModel schema = new SchemaModel();

        // Get name from title or use default
        if (root.has("title")) {
            schema.setName(root.get("title").getAsString());
        } else {
            schema.setName(defaultName);
        }

        // Get $id if present
        if (root.has("$id")) {
            schema.setId(root.get("$id").getAsString());
        }

        // Get description
        if (root.has("description")) {
            schema.setDescription(root.get("description").getAsString());
        }

        // Parse required fields
        if (root.has("required") && root.get("required").isJsonArray()) {
            JsonArray requiredArray = root.getAsJsonArray("required");
            for (JsonElement element : requiredArray) {
                schema.addRequired(element.getAsString());
            }
        }

        // Parse properties
        if (root.has("properties") && root.get("properties").isJsonObject()) {
            JsonObject properties = root.getAsJsonObject("properties");
            for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
                String propName = entry.getKey();
                JsonObject propDef = entry.getValue().getAsJsonObject();
                PropertyDefinition property = parseProperty(propName, propDef);
                schema.addProperty(property);
            }
        }

        // Check for allOf (inheritance)
        if (root.has("allOf") && root.get("allOf").isJsonArray()) {
            parseAllOf(schema, root.getAsJsonArray("allOf"));
        }

        logger.debug("Parsed schema: {}", schema);
        return schema;
    }

    private PropertyDefinition parseProperty(String name, JsonObject propDef) {
        PropertyDefinition property = new PropertyDefinition();
        property.setName(name);

        // Handle $ref
        if (propDef.has("$ref")) {
            String ref = propDef.get("$ref").getAsString();
            property.setRefType(extractRefName(ref));
            property.setType("object"); // References are treated as objects
            return property;
        }

        // Get type
        if (propDef.has("type")) {
            property.setType(propDef.get("type").getAsString());
        } else {
            property.setType("string"); // Default to string
        }

        // Get format
        if (propDef.has("format")) {
            property.setFormat(propDef.get("format").getAsString());
        }

        // Get description
        if (propDef.has("description")) {
            property.setDescription(propDef.get("description").getAsString());
        }

        // Get default value
        if (propDef.has("default")) {
            property.setDefaultValue(extractDefaultValue(propDef.get("default")));
        }

        // Handle enum
        if (propDef.has("enum") && propDef.get("enum").isJsonArray()) {
            List<String> enumValues = new ArrayList<>();
            for (JsonElement e : propDef.getAsJsonArray("enum")) {
                enumValues.add(e.getAsString());
            }
            property.setEnumValues(enumValues);
        }

        // Handle nested object
        if ("object".equals(property.getType()) && propDef.has("properties")) {
            JsonObject nestedProps = propDef.getAsJsonObject("properties");
            for (Map.Entry<String, JsonElement> entry : nestedProps.entrySet()) {
                PropertyDefinition nestedProp = parseProperty(entry.getKey(), entry.getValue().getAsJsonObject());
                property.addNestedProperty(nestedProp);
            }
        }

        // Handle array items
        if ("array".equals(property.getType()) && propDef.has("items")) {
            JsonObject items = propDef.getAsJsonObject("items");
            PropertyDefinition itemsDef = parseProperty("items", items);
            property.setItemsDefinition(itemsDef);
        }

        return property;
    }

    private void parseAllOf(SchemaModel schema, JsonArray allOf) {
        for (JsonElement element : allOf) {
            JsonObject obj = element.getAsJsonObject();

            // Handle $ref for parent type
            if (obj.has("$ref")) {
                String ref = obj.get("$ref").getAsString();
                schema.setParentType(extractRefName(ref));
            }

            // Handle inline properties
            if (obj.has("properties") && obj.get("properties").isJsonObject()) {
                JsonObject properties = obj.getAsJsonObject("properties");
                for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
                    PropertyDefinition property = parseProperty(entry.getKey(), entry.getValue().getAsJsonObject());
                    schema.addProperty(property);
                }
            }

            // Handle required from allOf
            if (obj.has("required") && obj.get("required").isJsonArray()) {
                for (JsonElement req : obj.getAsJsonArray("required")) {
                    schema.addRequired(req.getAsString());
                }
            }
        }
    }

    /**
     * Extract the type name from a $ref string.
     * Examples:
     *   "#/definitions/Address" -> "Address"
     *   "https://example.com/schemas/base.json" -> "base"
     */
    private String extractRefName(String ref) {
        if (ref.contains("/")) {
            String[] parts = ref.split("/");
            String lastPart = parts[parts.length - 1];
            // Remove .json extension if present
            if (lastPart.endsWith(".json")) {
                lastPart = lastPart.substring(0, lastPart.length() - 5);
            }
            return lastPart;
        }
        return ref;
    }

    private Object extractDefaultValue(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            } else if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsNumber();
            } else {
                return element.getAsString();
            }
        }
        return element.toString();
    }

    /**
     * Exception thrown when JSON Schema parsing fails.
     */
    public static class JsonSchemaParseException extends Exception {
        public JsonSchemaParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
