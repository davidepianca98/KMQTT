package io.github.davidepianca98.mqtt.packets.mqttv4

import io.github.davidepianca98.mqtt.MQTTException
import io.github.davidepianca98.mqtt.packets.ConnectAcknowledgeFlags
import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTConnack
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT4Connack(
    connectAcknowledgeFlags: ConnectAcknowledgeFlags,
    public val connectReturnCode: ConnectReturnCode
) : MQTTConnack(connectAcknowledgeFlags), MQTT4Packet {

    public companion object : MQTTDeserializer {

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
