# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Ignition module that creates UDT definitions from JSON Schema files, with MQTT-based schema updates. Built with Gradle (Kotlin DSL) and the Ignition Module SDK.

- **SDK Version**: 8.1.20
- **Java**: 11+ (toolchain configured for 11)
- **Build Plugin**: io.ia.sdk.modl v0.4.0

## Build Commands

**First-time setup** (required before first build):
```bash
./code-signing/generate-keystore.sh
```
This generates the keystore, certificate files, and `gradle.properties` needed for module signing.

**Build**:
```bash
./gradlew build           # Build .modl file
./gradlew clean build     # Clean and rebuild
```

Output: `build/Schema-Tag-Provider.modl`

## Architecture

### Module Scopes

This module is Gateway-only (scope `G`). Ignition modules use scope designators:
- **G** - Gateway (server-side)
- **C** - Vision Client
- **D** - Designer

| Subproject | Scope | Description |
|------------|-------|-------------|
| common     | G     | Shared code (currently Gateway-only) |
| gateway    | G     | Server-side code |

### Entry Point

**SchemaTagProviderGatewayHook** (`gateway/`) - Extends `AbstractGatewayModuleHook`, handles module lifecycle

**Module ID**: `com.theoremsystems.ignition.SchemaTagProvider`

### Data Flow

```
JSON Schema Input (File or MQTT)
         ↓
    SchemaCacheManager (caches to disk)
         ↓
    JsonSchemaParser → SchemaModel
         ↓
    UdtDefinitionBuilder (creates UDT JSON)
         ↓
    UdtSynchronizer (imports to TagProvider)
         ↓
    Ignition _types_ folder
```

### Key Components

- **TagProviderManager** - Main coordinator, orchestrates all components
- **SchemaCacheManager** - Local file cache, detects adds/updates/deletes
- **MqttSchemaListener** - MQTT client subscribing to schema topics
- **UdtSynchronizer** - Imports/removes UDT definitions via `TagProvider.importTagsAsync()`

## Dependencies

Ignition SDK dependencies use `compileOnly` (provided at runtime).

**Bundling third-party libraries**: Use `modlImplementation` (not `implementation`):
```kotlin
modlImplementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
modlImplementation("com.google.code.gson:gson:2.9.0")
```

## Configuration

Config file: `<Ignition Data Dir>/modules/schema-tag-provider/config.properties`

On macOS: `/usr/local/ignition/data/modules/schema-tag-provider/config.properties`

Defaults defined in: `gateway/.../config/ModuleSettings.java`

**Important**: Defaults only apply when creating a new config file. Adding properties to an existing file requires setting them explicitly.

## Testing

No automated tests. Manual testing:
1. Place JSON Schema files in: `<Ignition Data Dir>/modules/schema-tag-provider/schemas/`
2. Or publish to MQTT: `mosquitto_pub -t "ignition/schemas/Sensor" -f Sensor.json`
3. Check `_types_` folder in Tag Browser for UDTs
4. Logs: Gateway > Status > Logs (search "TagProviderManager")

## Key Implementation Details

### Periodic Cache Scanning
Uses `ExecutionManager.scheduleWithFixedDelay()`. `SchemaCacheManager.reload()` returns deleted schema names by comparing keysets before/after reload.

### UDT Import/Remove
Uses `TagProvider.importTagsAsync()` with `CollisionPolicy.Overwrite` for imports. Deletions (when `allowdelete=true`) remove from `_types_/<SchemaName>` path.
