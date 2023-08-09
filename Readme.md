# KMQTT

**KMQTT** is a Kotlin Multiplatform MQTT 3.1.1/5.0 Client and Broker, with the objective of targeting the most possible build targets.


## Client features
:x: = To Do  
:white_check_mark: = Supported  
:heavy_plus_sign: = Work In Progress

|        Platform         |     MQTT 3.1.1     |      MQTT 5.0      |        TCP         |        TLS         |     Websocket      |
|:-----------------------:|:------------------:|:------------------:|:------------------:|:------------------:|:------------------:|
|           JVM           | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|       Windows X64       | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|        Linux X64        | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|       Linux ARM64       | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|         Node.js         | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|         iOS X64         | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|        iOS ARM64        | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|   iOS Simulator ARM64   | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|        macOS X64        | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|       macOS ARM64       | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|        tvOS X64         | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|       tvOS ARM64        | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|  tvOS Simulator ARM64   | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|       watchOS X64       | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|      watchOS ARM32      | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
|      watchOS ARM64      | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |
| watchOS Simulator ARM64 | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: | :white_check_mark: |

## Broker features
:x: = To Do  
:white_check_mark: = Supported  
:heavy_plus_sign: = Work In Progress

|  Platform   |     MQTT 3.1.1     |      MQTT 5.0       |         TCP         |        TLS         |     Websocket      |    Clustering     |
|:-----------:|:------------------:|:-------------------:|:-------------------:|:------------------:|:------------------:|:-----------------:|
|     JVM     | :white_check_mark: | :white_check_mark:  | :white_check_mark:  | :white_check_mark: | :white_check_mark: | :heavy_plus_sign: |
| Windows X64 | :white_check_mark: | :white_check_mark:  | :white_check_mark:  | :white_check_mark: | :white_check_mark: |        :x:        |
|  Linux X64  | :white_check_mark: | :white_check_mark:  | :white_check_mark:  | :white_check_mark: | :white_check_mark: |        :x:        |
| Linux ARM64 | :white_check_mark: | :white_check_mark:  | :white_check_mark:  | :white_check_mark: | :white_check_mark: |        :x:        |
|   Node.js   | :white_check_mark: | :white_check_mark:  | :white_check_mark:  | :white_check_mark: | :white_check_mark: |        :x:        |
|  macOS X64  | :white_check_mark: | :white_check_mark:  | :white_check_mark:  | :white_check_mark: | :white_check_mark: |        :x:        |
| macOS ARM64 | :white_check_mark: | :white_check_mark:  | :white_check_mark:  | :white_check_mark: | :white_check_mark: |        :x:        |




## Getting Started with the client

### Library

#### Gradle

NOTE: Apple targets are not yet available through JitPack, so for now the library is published on GitHub packages which requires login credentials also for reading.

Setup GitHub credentials:
1. Open ~/.gradle/gradle.properties
2. Add the line `mavenUsername=yourGithubUsername`
3. Add the line `mavenPassword=yourGithubToken` (instructions to generate it https://docs.github.com/en/enterprise-server@3.6/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)

Alternatively it is possible to follow these steps to build the library locally:
1. Clone the repository.
2. Run the gradle task `publishToMavenLocal`.
3. Replace the GitHub `maven` block with `mavenLocal()` in the `repositories` block in your `build.gradle.kts` file.
4. Continue with the following guide.

If you are getting an error saying that OpenSSL hasn't been found, please copy the correct file from https://github.com/davidepianca98/KMQTT/tree/master/kmqtt-common/src/nativeInterop in your project's main directory.

##### Kotlin Multiplatform plugin
On the Kotlin Multiplatform plugin you only need to require the dependency on the common source set and the platform specific parts will be automatically imported.
```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/davidepianca98/KMQTT")
        credentials(PasswordCredentials::class)
    }
}

kotlin {
    jvm()
    mingwX64()
    macosX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.github.davidepianca98:kmqtt-common:0.4.2")
                implementation("com.github.davidepianca98:kmqtt-client:0.4.2")
            }
        }
    }
}
```

NOTE: If you want to use Jitpack, replace "com.github.davidepianca98" with "com.github.davidepianca98.KMQTT" in the `implementation` statements.

##### Single platform project
```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/davidepianca98/KMQTT")
        credentials(PasswordCredentials::class)
    }
}
dependencies {
    implementation("com.github.davidepianca98:kmqtt-common-jvm:0.4.2")
    implementation("com.github.davidepianca98:kmqtt-client-jvm:0.4.2")
}
```

Replace jvm with js, linuxx64, linuxarm64, mingwx64, macosx64, macosarm64, iosarm64, iosX64, iossimulatorarm64, tvossimulatorarm64, tvosx64, tvosarm64, watchosarm32, watchosarm64, watchosx64, watchossimulatorarm64 based on the desired target.

NOTE: If you want to use Jitpack, replace "com.github.davidepianca98" with "com.github.davidepianca98.KMQTT" in the `implementation` statements.

#### Quick start code example
This code starts the MQTT client on port 1883 without TLS encryption. You can play with MQTTClient() constructor parameters to set the various settings
```kotlin
fun main() {
    val client = MQTTClient(
        5,
        "test.mosquitto.org",
        1883,
        null
    ) {
        println(it.payload?.toByteArray()?.decodeToString())
    }
    client.subscribe(listOf(Subscription("/randomTopic", SubscriptionOptions(Qos.EXACTLY_ONCE))))
    client.publish(false, Qos.EXACTLY_ONCE, "/randomTopic", "hello".encodeToByteArray().toUByteArray())
    client.run() // Blocking method, use step() if you don't want to block the thread
}
```

#### TLS code example
The certificates and key must be in PEM format, the password can be null
```kotlin
fun main() {
    val client = MQTTClient(
        5,
        "test.mosquitto.org",
        8883,
        TLSClientSettings(
            serverCertificatePath = "mosquitto.org.crt",
        )
    ) {
        println(it.payload?.toByteArray()?.decodeToString())
    }
    client.subscribe(listOf(Subscription("/randomTopic", SubscriptionOptions(Qos.EXACTLY_ONCE))))
    client.publish(false, Qos.EXACTLY_ONCE, "/randomTopic", "hello".encodeToByteArray().toUByteArray())
    client.run() // Blocking method, use step() if you don't want to block the thread
}
```



## Getting Started with the broker

### Executables
You can download the executables for your platform under the release tab

#### Program Arguments

|     Argument      | Default Value |                                                       Description                                                       |
|:-----------------:|:-------------:|:-----------------------------------------------------------------------------------------------------------------------:|
|        -h         |   127.0.0.1   |                                         Interface address to bind the server to                                         |
|        -p         |     1883      |                                                Server port to listen to                                                 |
| --max-connections |      128      |                                    The maximum number of TCP connections to support                                     |
|    --key-store    |     null      |     The path to the PKCS12 keystore containing the private key and the certificate for TLS, if null TLS is disabled     |
|  --key-store-psw  |     null      | The password of the PKCS12 keystore indicated in --key-store, if the keystore has no password set, don't set the option |
|       --wsp       |     null      |                                             The WebSocket port to listen to                                             |

### Library

#### Gradle

NOTE: Apple targets are not yet available through JitPack, so for now the library is published on GitHub packages which requires login credentials also for reading.

Setup GitHub credentials:
1. Open ~/.gradle/gradle.properties
2. Add the line `mavenUsername=yourGithubUsername`
3. Add the line `mavenPassword=yourGithubToken` (instructions to generate it https://docs.github.com/en/enterprise-server@3.6/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)

Alternatively it is possible to follow these steps to build the library locally:
1. Clone the repository.
2. Run the gradle task `publishToMavenLocal`.
3. Replace the GitHub `maven` block with `mavenLocal()` in the `repositories` block in your `build.gradle.kts` file.
4. Continue with the following guide.

If you are getting an error saying that OpenSSL hasn't been found, please copy the correct file from https://github.com/davidepianca98/KMQTT/tree/master/kmqtt-common/src/nativeInterop in your project's main directory.

##### Kotlin Multiplatform plugin
On the Kotlin Multiplatform plugin you only need to require the dependency on the common source set and the platform specific parts will be automatically imported.
```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/davidepianca98/KMQTT")
        credentials(PasswordCredentials::class)
    }
}

kotlin {
    jvm()
    mingwX64()
    macosX64()
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.github.davidepianca98:kmqtt-common:0.4.2")
                implementation("com.github.davidepianca98:kmqtt-broker:0.4.2")
            }
        }
    }
}
```

NOTE: If you want to use Jitpack, replace "com.github.davidepianca98" with "com.github.davidepianca98.KMQTT" in the `implementation` statements.

##### Single platform project
```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/davidepianca98/KMQTT")
        credentials(PasswordCredentials::class)
    }
}
dependencies {
    implementation("com.github.davidepianca98:kmqtt-common-jvm:0.4.2")
    implementation("com.github.davidepianca98:kmqtt-broker-jvm:0.4.2")
}
```

Replace jvm with js, linuxx64, linuxarm64, macosarm64, macosx64, mingwx64 based on the desired target.

NOTE: If you want to use Jitpack, replace "com.github.davidepianca98" with "com.github.davidepianca98.KMQTT" in the `implementation` statements.

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
        override fun authenticate(clientId: String, username: String?, password: UByteArray?): Boolean {
            // TODO Implement your authentication method    
            return username == "user" && password?.toByteArray()?.decodeToString() == "pass"
        }
    })
    broker.listen() // Blocking method, use step() if you don't want to block the thread
}
```

#### Authorization code example

```kotlin
fun main() {
    val broker = Broker(authorization = object : Authorization {
        override fun authorize(
            clientId: String,
            username: String?,
            password: UByteArray?, // != null only if savePassword set to true in the broker constructor
            topicName: String,
            isSubscription: Boolean,
            payload: UByteArray?
        ): Boolean {
            // TODO Implement your authorization method    
            return topicName == "$clientId/topic"
        }
    })
    broker.listen() // Blocking method, use step() if you don't want to block the thread
}
```

#### Message interceptor code example

```kotlin
fun main() {
    val broker = Broker(packetInterceptor = object : PacketInterceptor {
        override fun packetReceived(clientId: String, username: String?, password: UByteArray?, packet: MQTTPacket) {
            when (packet) {
                is MQTTConnect -> println(packet.protocolName)
                is MQTTPublish -> println(packet.topicName)
            }
        }
    })
    broker.listen() // Blocking method, use step() if you don't want to block the thread
}
```

#### Internal publish code example
```kotlin
fun main() {
    val broker = Broker()
    broker.publish(
        retain = false,
        topicName = "test/",
        qos = Qos.AT_MOST_ONCE,
        properties = MQTTProperties(),
        "testPayload".toByteArray().toUByteArray()
    )
    broker.listen() // Blocking method, use step() if you don't want to block the thread
}
```

#### Other advanced functionality

MQTT5 Enhanced Authentication: set the `enhancedAuthenticationProviders` Broker constructor parameter, implementing the
provider interface `EnhancedAuthenticationProvider`.

Session persistence: set the `persistence` Broker constructor parameter, implementing `Persistence` interface.

Bytes metrics: set the `bytesMetrics` Broker constructor parameter, implementing `BytesMetrics` interface.  
