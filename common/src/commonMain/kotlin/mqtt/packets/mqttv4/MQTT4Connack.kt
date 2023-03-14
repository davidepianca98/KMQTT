package mqtt.packets.mqttv4

import mqtt.MQTTException
import mqtt.packets.ConnectAcknowledgeFlags
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTConnack
import mqtt.packets.mqttv5.ReasonCode
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTT4Connack(
    connectAcknowledgeFlags: ConnectAcknowledgeFlags,
    val connectReturnCode: ConnectReturnCode
) : mqtt.packets.mqtt.MQTTConnack(connectAcknowledgeFlags), MQTT4Packet {

    companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Connack {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)

            val connectAcknowledgeFlags = when (inStream.readByte()) {
                0u -> ConnectAcknowledgeFlags(false)
                1u -> ConnectAcknowledgeFlags(true)
                else -> throw MQTTException(ReasonCode.MALFORMED_PACKET)
            }
            val connectReturnCode =
                ConnectReturnCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(
                    ReasonCode.PROTOCOL_ERROR
                )

            return MQTT4Connack(
                connectAcknowledgeFlags,
                connectReturnCode
            )
        }

    }

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        val connectFlags =
            if (connectAcknowledgeFlags.sessionPresentFlag && connectReturnCode == ConnectReturnCode.CONNECTION_ACCEPTED) 1u else 0u
        outStream.write(connectFlags.toUByte())
        outStream.write(connectReturnCode.value.toUByte())

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.CONNACK, 0)
    }
}
