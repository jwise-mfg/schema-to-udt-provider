# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Ignition module for Inductive Automation's Ignition SCADA platform. It's a multi-scope module built with Gradle (Kotlin DSL) and the Ignition Module SDK.

- **SDK Version**: 8.1.20
- **Java**: 11+
- **Gradle**: 7.6
- **Build Plugin**: io.ia.sdk.modl v0.4.0

## Build Commands

Requires Java 11+ to build (set JAVA_HOME or use sdkman):

```bash
export JAVA_HOME=~/.sdkman/candidates/java/17.0.13-tem  # if needed
./gradlew build           # Build all modules into .modl file
./gradlew clean build     # Clean and rebuild
```

The build produces `build/Schema-Tag-Provider.modl` which can be installed in Ignition.

## Architecture

### Module Scopes

Ignition modules use scope designators to specify where code runs:

| Subproject | Scope | Description |
|------------|-------|-------------|
| common     | GCD   | Shared code available in Gateway, Client, and Designer |
| gateway    | G     | Server-side code running in the Gateway |
| client     | CD    | Vision Client (also included in Designer for testing) |
| designer   | D     | Designer IDE only |

### Hook Classes

Each scope has a hook class that serves as the entry point:

- **TestTagProviderGatewayHook** (`gateway/`) - Extends `AbstractGatewayModuleHook`, handles server-side lifecycle
- **TestTagProviderClientHook** (`client/`) - Extends `AbstractClientModuleHook`, handles Vision client lifecycle
- **TestTagProviderDesignerHook** (`designer/`) - Extends `AbstractDesignerModuleHook`, handles Designer IDE lifecycle
- **TestTagProviderModule** (`common/`) - Contains the module ID constant shared across all scopes

### Module ID

The module is identified by: `com.theoremsystems.ignition.testtagprovider.TestTagProvider`

## Dependencies

All Ignition SDK dependencies use `compileOnly` scope since Ignition provides them at runtime. The SDK libraries come from Inductive Automation's Nexus repository (configured in the Gradle plugin).

**Important**: Third-party libraries that need to be bundled in the module must use `modlImplementation` (not `implementation` or `compileOnly`). This ensures they are packaged inside the `.modl` file and available at runtime.

Example in `gateway/build.gradle.kts`:
```kotlin
modlImplementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
modlImplementation("com.google.code.gson:gson:2.9.0")
```

## Gateway Module Structure

The gateway module contains the main functionality:

```
gateway/src/main/java/.../gateway/
├── TestTagProviderGatewayHook.java  # Module entry point
├── TagProviderManager.java          # Main coordinator
├── config/
│   ├── ModuleSettings.java          # Configuration defaults
│   └── ConfigLoader.java            # Properties file loader
├── mqtt/
│   ├── MqttSchemaListener.java      # MQTT client
│   └── MqttConnectionConfig.java    # Connection settings
├── schema/
│   ├── SchemaCacheManager.java      # Local file cache
│   ├── JsonSchemaParser.java        # JSON Schema parsing
│   ├── SchemaModel.java             # Schema representation
│   └── DataTypeMapper.java          # JSON type → Ignition type
└── udt/
    ├── UdtDefinitionBuilder.java    # Builds UDT JSON
    └── UdtSynchronizer.java         # Imports to TagProvider
```

## Configuration

Config file location: `<Ignition Data Dir>/modules/Schema-Tag-provider/config.properties`

Default values are in: `gateway/.../config/ModuleSettings.java`

Key configuration options:
- `tag.provider.name` - Target tag provider (default: `default`)
- `schema.cache.scan.interval.seconds` - Cache scan frequency (default: `30`, set to `0` to disable)
- `tag.provider.allowdelete` - Whether to remove UDTs when schemas are deleted (default: `true`)
- `mqtt.enabled` - Enable/disable MQTT listener (default: `true`)

**Important**: The module reads config on startup. If you add new properties to an existing config file, you must set them explicitly - defaults only apply when creating a new config file.

## Testing the Module

1. Place JSON Schema files in: `<Ignition Data Dir>/modules/Schema-Tag-provider/schemas/`
2. Or publish to MQTT topic: `ignition/schemas/<SchemaName>`
3. Check `_types_` folder in Tag Browser for created UDTs
4. View logs: Gateway > Status > Logs (search for "TagProviderManager")

## Key Implementation Details

### Periodic Cache Scanning
The module uses Ignition's `ExecutionManager.scheduleWithFixedDelay()` to periodically scan the cache directory for changes. The `SchemaCacheManager.reload()` method returns a set of deleted schema names by comparing schemas before and after reload.

### Schema Deletion Flow
1. `SchemaCacheManager.reload()` detects deleted schemas by comparing keysets
2. `TagProviderManager.scanAndSyncCache()` checks `settings.isAllowDelete()`
3. If allowed, calls `UdtSynchronizer.removeUdtDefinition()` for each deleted schema
4. Same logic applies in `onSchemaDeleted()` for MQTT-triggered deletions

### UDT Import/Remove
Uses `TagProvider.importTagsAsync()` with appropriate `CollisionPolicy`:
- Import: `CollisionPolicy.Overwrite` for updates
- Remove: Deletes from `_types_/<SchemaName>` path
