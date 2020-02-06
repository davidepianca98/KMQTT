import mqtt.broker.Broker
import mqtt.broker.PacketInterceptor
import mqtt.packets.mqttv5.MQTT5Packet
import mqtt.packets.mqttv5.MQTTConnect
import mqtt.packets.mqttv5.MQTTPublish
import socket.tls.TLSSettings
import java.nio.ByteBuffer

actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

fun ByteBuffer.toUByteArray(): UByteArray {
    val length = remaining()
    val array = ByteArray(length)
    get(array, 0, length)
    return array.toUByteArray()
}

fun main() {
    Broker(
        tlsSettings = TLSSettings(keyStoreFilePath = "keyStore.p12", keyStorePassword = "changeit"),
        port = 8883,
        packetInterceptor = object : PacketInterceptor {
            override fun packetReceived(packet: MQTT5Packet) {
                when (packet) {
                    is MQTTConnect -> println(packet.protocolName)
                    is MQTTPublish -> println(packet.topicName)
                }
            }
        }
    ).listen()
}
