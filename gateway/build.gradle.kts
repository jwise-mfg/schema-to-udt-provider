plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(11))
    }
}

dependencies {
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:${rootProject.extra["sdk_version"]}")
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:${rootProject.extra["sdk_version"]}")
    compileOnly(project(":common"))

    // MQTT Client - Eclipse Paho (modlImplementation bundles it in the .modl file)
    modlImplementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // Gson for JSON parsing (bundled in .modl file)
    modlImplementation("com.google.code.gson:gson:2.9.0")
}
