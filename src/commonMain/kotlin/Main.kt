import mqtt.Broker
import socket.tls.TLSSettings

fun main() {
    Broker(
        tlsSettings = TLSSettings(keyStoreFilePath = "keyStore.p12", keyStorePassword = "changeit"),
        port = 8883
    ).listen()
}
