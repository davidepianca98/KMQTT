import mqtt.broker.Broker
import mqtt.broker.interfaces.Authorization
import socket.tls.TLSSettings

fun main() {
    Broker(
        serverKeepAlive = 60,
        authorization = object : Authorization {
            override fun authorize(
                clientId: String,
                username: String?,
                password: UByteArray?,
                topicName: String,
                isSubscription: Boolean,
                payload: UByteArray?
            ): Boolean {
                return topicName != "test/nosubscribe"
            }
        },
        webSocketPort = 80,
        maximumPacketSize = 128000u,
        port = 8883,
        tlsSettings = TLSSettings(keyStoreFilePath = "kmqtt-broker/docker/linux/keyStore.p12", keyStorePassword = "changeit")
    ).listen()
}
