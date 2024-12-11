package io.github.davidepianca98.mqtt.packets.mqttv5

import io.github.davidepianca98.mqtt.MQTTException
import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTUnsubscribe
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT5Unsubscribe(
    packetIdentifier: UInt,
    topicFilters: List<String>,
    public val properties: MQTT5Properties = MQTT5Properties()
) : MQTTUnsubscribe(packetIdentifier, topicFilters), MQTT5Packet {

    public companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.USER_PROPERTY
        )

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT5Unsubscribe {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetIdentifier = inStream.read2BytesInt()
            val properties = inStream.deserializeProperties(validProperties)
            val topicFilters = mutableListOf<String>()
            while (inStream.available() > 0) {
                topicFilters += inStream.readUTF8String()
            }
            return MQTT5Unsubscribe(packetIdentifier, topicFilters, properties)
        }

        override fun checkFlags(flags: Int) {
            if (flags.flagsBit(0) != 0 ||
                flags.flagsBit(1) != 1 ||
                flags.flagsBit(2) != 0 ||
                flags.flagsBit(3) != 0
            )
                throw MQTTException(ReasonCode.MALFORMED_PACKET)
        }
    }

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)
        outStream.write(properties.serializeProperties(validProperties))

        topicFilters.forEach {
            outStream.writeUTF8String(it)
        }

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.UNSUBSCRIBE, 2)
    }
}
