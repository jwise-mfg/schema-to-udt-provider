package com.theoremsystems.ignition.schematagprovider.gateway.schema;

/**
 * Maps JSON Schema types to Ignition DataType names.
 */
public class DataTypeMapper {

    /**
     * Maps a JSON Schema type and optional format to an Ignition DataType name.
     *
     * @param jsonType   The JSON Schema type (string, integer, number, boolean, etc.)
     * @param format     Optional format specifier (date-time, date, etc.)
     * @return The Ignition DataType name as a string
     */
    public static String mapToIgnitionType(String jsonType, String format) {
        if (jsonType == null) {
            return "String";
        }

        switch (jsonType.toLowerCase()) {
            case "string":
                return mapStringFormat(format);
            case "integer":
                return "Int4";
            case "number":
                return mapNumberFormat(format);
            case "boolean":
                return "Boolean";
            case "array":
                return "DataSet";
            case "object":
                // Objects become nested UDTs, return null to indicate special handling
                return null;
            default:
                return "String";
        }
    }

    private static String mapStringFormat(String format) {
        if (format == null) {
            return "String";
        }

        switch (format.toLowerCase()) {
            case "date-time":
            case "datetime":
                return "DateTime";
            case "date":
                return "DateTime";  // Ignition uses DateTime for dates too
            case "time":
                return "String";    // No dedicated time type in Ignition
            case "byte":
                return "Int1";
            case "binary":
                return "String";    // Base64 encoded as string
            default:
                return "String";
        }
    }

    private static String mapNumberFormat(String format) {
        if (format == null) {
            return "Float8";
        }

        switch (format.toLowerCase()) {
            case "float":
                return "Float4";
            case "double":
                return "Float8";
            case "int32":
            case "int":
                return "Int4";
            case "int64":
            case "long":
                return "Int8";
            default:
                return "Float8";
        }
    }

    /**
     * Returns true if the JSON type represents an object that should become a nested UDT.
     */
    public static boolean isNestedType(String jsonType) {
        return "object".equalsIgnoreCase(jsonType);
    }

    /**
     * Returns true if the JSON type represents an array.
     */
    public static boolean isArrayType(String jsonType) {
        return "array".equalsIgnoreCase(jsonType);
    }
}
