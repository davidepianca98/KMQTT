package io.github.davidepianca98.mqtt.packets.mqttv4

import io.github.davidepianca98.mqtt.MQTTException
import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTUnsubscribe
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream

public class MQTT4Unsubscribe(
    packetIdentifier: UInt,
    topicFilters: List<String>
) : MQTTUnsubscribe(packetIdentifier, topicFilters), MQTT4Packet {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)

        topicFilters.forEach {
            outStream.writeUTF8String(it)
        }

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.UNSUBSCRIBE, 2)
    }

    public companion object : MQTTDeserializer {

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT4Unsubscribe {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetIdentifier = inStream.read2BytesInt()
            val topicFilters = mutableListOf<String>()
            while (inStream.available() > 0) {
                topicFilters += inStream.readUTF8String()
            }
            return MQTT4Unsubscribe(packetIdentifier, topicFilters)
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
}
