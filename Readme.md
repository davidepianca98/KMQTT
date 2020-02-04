# KMQTT

**KMQTT** is a Kotlin Multiplatform MQTT 5.0 Broker, with the objective of targeting the most possible build targets.

## Getting Started

#### Gradle Maven
TODO

#### Quick start code example
This code starts the MQTT broker on port 1883 without TLS encryption. You can play with Broker() constructor parameters to set the various settings
```kotlin
fun main() {
    Broker().listen()
}
```

#### TLS code example
The keystore must be in PKCS12 format, the keystore password can be null
```kotlin
fun main() {
    val broker = Broker(
        tlsSettings = TLSSettings(keyStoreFilePath = "keyStore.p12", keyStorePassword = "password"),
        port = 8883
    ).listen()
    broker.listen()
}
```

#### Authentication code example
```kotlin
fun main() {
    val broker = Broker(authentication = object : Authentication {
        override fun authenticate(username: String?, password: UByteArray?): Boolean {
            // TODO Implement your authentication method    
            return username == "user" && password?.toByteArray()?.decodeToString() == "pass"
        }
    })
    broker.listen()
}
```

## Features
| Platform    | MQTT 3.1.1 | MQTT 5.0           | TCP                | TLS                | Websocket |
|   :---:     |    :---:   |  :---:             | :---:              | :---:              | :---:     |
| JVM         | TODO       | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | TODO      |
| Windows     | TODO       | :heavy_check_mark: | :heavy_check_mark: | WIP                | TODO      |
| Linux X64   | TODO       | :heavy_check_mark: | TODO               | TODO               | TODO      |
| Linux ARM32 | TODO       | :heavy_check_mark: | TODO               | TODO               | TODO      |
