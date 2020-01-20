package mqtt.packets

import mqtt.MQTTException
import java.io.ByteArrayInputStream

class MQTTUnsubscribe(
    val packetIdentifier: UInt,
    val topicFilters: List<String>,
    val properties: MQTTProperties = MQTTProperties()
) : MQTTPacket {

    override fun toByteArray(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object : MQTTDeserializer {

        val validProperties = listOf(
            Property.USER_PROPERTY
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTUnsubscribe {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)
            val packetIdentifier = inStream.read2BytesInt()
            val properties = inStream.deserializeProperties(validProperties)
            val topicFilters = mutableListOf<String>()
            while (inStream.available() > 0) {
                topicFilters += inStream.readUTF8String()
            }
            return MQTTUnsubscribe(packetIdentifier, topicFilters, properties)
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
