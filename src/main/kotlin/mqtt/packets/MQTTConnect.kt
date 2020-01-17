package mqtt.packets

import mqtt.MalformedPacketException
import java.io.ByteArrayInputStream

class MQTTConnect(
    val protocolName: String,
    val protocolVersion: Int,
    val connectFlags: ConnectFlags,
    val keepAlive: Int,
    val properties: MQTTProperties
) : MQTTPacket {

    companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.SESSION_EXPIRY_INTERVAL,
            Property.AUTHENTICATION_METHOD,
            Property.AUTHENTICATION_DATA,
            Property.REQUEST_INFORMATION,
            Property.REQUEST_RESPONSE_INFORMATION,
            Property.RECEIVE_MAXIMUM,
            Property.TOPIC_ALIAS_MAXIMUM,
            Property.USER_PROPERTY,
            Property.MAXIMUM_PACKET_SIZE
        )

        data class ConnectFlags(
            val userNameFlag: Boolean,
            val passwordFlag: Boolean,
            val willRetain: Boolean,
            val willQos: Int,
            val willFlag: Boolean,
            val cleanStart: Boolean,
            val reserved: Boolean
        )

        private fun connectFlags(byte: Int): ConnectFlags {
            val reserved = (byte and 1) == 1
            if (reserved)
                throw MalformedPacketException(ReasonCodes.MALFORMED_PACKET)
            return ConnectFlags(
                ((byte shl 7) and 1) == 1,
                ((byte shl 6) and 1) == 1,
                ((byte shl 5) and 1) == 1,
                ((byte shl 4) and 1) or ((byte shl 3) and 1),
                ((byte shl 2) and 1) == 1,
                ((byte shl 1) and 1) == 1,
                reserved
            )
        }

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTConnect {
            checkFlags(flags)

            val inStream = ByteArrayInputStream(data)
            val protocolName = inStream.readUTF8String()
            if (protocolName != "MQTT")
                throw MalformedPacketException(ReasonCodes.UNSUPPORTED_PROTOCOL_VERSION)
            val protocolVersion = inStream.read()
            if (protocolVersion != 5)
                throw MalformedPacketException(ReasonCodes.UNSUPPORTED_PROTOCOL_VERSION)

            val connectFlags = connectFlags(inStream.read())
            val keepAlive = inStream.read2BytesInt()

            val properties = inStream.deserializeProperties(validProperties)

            return MQTTConnect(protocolName, protocolVersion, connectFlags, keepAlive, properties)
        }

    }
}
