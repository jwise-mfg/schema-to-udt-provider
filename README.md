# Schema Tag Provider Module

An Ignition module that creates UDT definitions from JSON Schema files, with MQTT-based schema updates.

## Features

- Converts JSON Schema files to Ignition UDT definitions
- Listens for schema updates via MQTT
- Caches schemas locally for persistence
- Periodic cache scanning detects new/updated/deleted schemas
- Automatic UDT removal when schemas are deleted (configurable)
- Supports nested objects and type mapping

## Building

Requires Java 17+:

```bash
./gradlew build
```

Output: `build/Schema-Tag-Provider.modl`

## Configuration

On first startup, a config file is created at:
```
<Ignition Data Dir>/modules/schema-tag-provider/config.properties
```

### Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `mqtt.broker.url` | MQTT broker URL | `tcp://localhost:1883` |
| `mqtt.client.id` | MQTT client identifier | `ignition-schema-provider` |
| `mqtt.topic` | Topic to subscribe for schemas | `ignition/schemas/#` |
| `mqtt.username` | MQTT username (optional) | `` |
| `mqtt.password` | MQTT password (optional) | `` |
| `mqtt.qos` | MQTT QoS level | `1` |
| `mqtt.enabled` | Enable/disable MQTT listener | `true` |
| `schema.cache.path` | Local schema cache directory | `modules/schema-tag-provider/schemas` |
| `schema.cache.scan.interval.seconds` | How often to scan cache for changes (0 to disable) | `30` |
| `tag.provider.name` | Target tag provider for UDTs | `default` |
| `tag.provider.allowdelete` | Remove UDTs when schemas are deleted | `true` |

### Changing Defaults

Default values are defined in:
```
gateway/src/main/java/com/theoremsystems/ignition/testtagprovider/gateway/config/ModuleSettings.java
```

## Usage

1. Install the module in Ignition Gateway
2. Add JSON Schema files using one of the methods below
3. UDT definitions will appear in the `_types_` folder of the configured tag provider

### Method 1: Drop JSON Schema Files in Cache Directory

Create schema files in the cache folder:
```
<Ignition Data Dir>/modules/schema-tag-provider/schemas/
```

Example `Sensor.json`:
```json
{
  "title": "Sensor",
  "type": "object",
  "properties": {
    "temperature": {
      "type": "number",
      "description": "Temperature in Celsius"
    },
    "humidity": {
      "type": "number",
      "description": "Relative humidity percentage"
    },
    "status": {
      "type": "string"
    },
    "online": {
      "type": "boolean"
    }
  }
}
```

The module scans the cache directory periodically (default: every 30 seconds) and automatically syncs changes. You can also restart the module to pick up new files immediately.

### Method 2: Publish via MQTT

Publish a JSON Schema to the configured MQTT topic (default: `ignition/schemas/#`):

```bash
mosquitto_pub -t "ignition/schemas/Sensor" -f Sensor.json
```

The schema name is derived from the topic (e.g., `ignition/schemas/Sensor` → `Sensor`).

### Verifying UDT Creation

1. Open Ignition Designer
2. Go to Tag Browser
3. Look in the `_types_` folder of your configured tag provider
4. You should see UDT definitions matching your schema names

### Checking Logs

View module logs in Gateway: Status > Logs

Search for "TagProviderManager" or "Schema Tag Provider" to see startup messages and schema processing.

## JSON Schema to UDT Type Mapping

| JSON Schema Type | Ignition DataType |
|------------------|-------------------|
| `string` | `String` |
| `string` (format: date-time) | `DateTime` |
| `integer` | `Int4` |
| `number` | `Float8` |
| `boolean` | `Boolean` |
| `object` | Nested UDT |
| `array` | `DataSet` |

## Schema Deletion Behavior

When a schema file is deleted from the cache folder (or a delete message is received via MQTT):

- **`tag.provider.allowdelete=true`** (default): The corresponding UDT definition is removed from Ignition
- **`tag.provider.allowdelete=false`**: The UDT definition is preserved in Ignition even after the schema is deleted

This allows you to prevent accidental UDT removal in production environments.

**Note**: If you add `tag.provider.allowdelete` to an existing config file, you must set it explicitly. The module only uses the default value when creating a new config file.
