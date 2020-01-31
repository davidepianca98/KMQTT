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
TODO

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
| Platform    | MQTT 3.1.1 | MQTT 5.0           | TCP                | TLS   | Websocket |
|   :---:     |    :---:   |  :---:             | :---:              | :---: | :---:     |
| JVM         | TODO       | :heavy_check_mark: | :heavy_check_mark: | WIP   | TODO      |
| Windows     | TODO       | :heavy_check_mark: | :heavy_check_mark: | TODO  | TODO      |
| Linux X64   | TODO       | :heavy_check_mark: | TODO               | TODO  | TODO      |
| Linux ARM32 | TODO       | :heavy_check_mark: | TODO               | TODO  | TODO      |
