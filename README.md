# Schema Tag Provider Module

An Ignition module that creates UDT definitions from JSON Schema files, with MQTT-based schema updates.

## Features

- Converts JSON Schema files to Ignition UDT definitions
- Listens for schema updates via MQTT
- Caches schemas locally for persistence
- Periodic cache scanning detects new/updated/deleted schemas
- Automatic UDT removal when schemas are deleted (configurable)
- Supports nested objects and type mapping

## Development Environment

### Project Structure

```
schema-tag-provider/
├── build.gradle.kts          # Main build configuration
├── settings.gradle           # Gradle settings
├── gradle.properties         # Signing configuration (generated, not in git)
├── gradle.properties.example # Example signing config
├── common/                   # Shared code (Gateway scope)
│   └── src/main/java/...
├── gateway/                  # Gateway-scope module code
│   └── src/main/java/
│       └── .../gateway/
│           ├── SchemaTagProviderGatewayHook.java  # Module entry point
│           ├── TagProviderManager.java            # Main coordinator
│           ├── config/        # Configuration loading
│           ├── mqtt/          # MQTT listener
│           ├── schema/        # JSON Schema parsing
│           └── udt/           # UDT building and sync
└── code-signing/             # Module signing tools
    ├── README.md             # Signing documentation
    └── generate-keystore.sh  # Signing file generator
```

### Code Signing Setup

Ignition modules must be signed. This project includes a helper script to generate self-signed certificates.

**First-time setup:**

```bash
./code-signing/generate-keystore.sh
```

This interactive script will:
1. Prompt for certificate details (name, organization, etc.)
2. Generate `code-signing/keystore.jks` (private key)
3. Generate `code-signing/codesigning.pem` (certificate)
4. Generate `code-signing/codesigning.p7b` (certificate chain)
5. Generate `gradle.properties` (signing configuration)

The signing files are gitignored - each developer generates their own.

**Note:** Self-signed modules show a warning in Ignition when installing, but work normally.

### Building

Requires Java 17+:

```bash
./gradlew build
```

If signing files are missing, the build will fail with instructions to run the setup script.

Output: `build/Schema-Tag-Provider.modl`

## Installation

Use the Ignition Gateway settings page, navigate to Modules on the left. 

Scroll to the bottom and click "Install or Upgrade a Module..."

Browse to the signed .modl file generated during the build, and accept the self-signed certificate.

## Configuration

On first startup, a config file is created at:
```
<Ignition Data Dir>/modules/schema-tag-provider/config.properties
```

**Note:** on macOS `<IgnitionDataDir>` is /usr/local/ignition/data

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
gateway/src/main/java/.../gateway/config/ModuleSettings.java
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
