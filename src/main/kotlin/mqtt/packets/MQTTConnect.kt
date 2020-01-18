package mqtt.packets

import mqtt.MalformedPacketException
import java.io.ByteArrayInputStream

data class MQTTConnect(
    val protocolName: String,
    val protocolVersion: Int,
    val connectFlags: ConnectFlags,
    val keepAlive: Int,
    val properties: MQTTProperties,
    val clientID: String,
    val willProperties: MQTTProperties?,
    val willTopic: String?,
    val willPayload: ByteArray?,
    val userName: String?,
    val password: ByteArray?
) : MQTTPacket {

    companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.SESSION_EXPIRY_INTERVAL,
            Property.AUTHENTICATION_METHOD,
            Property.AUTHENTICATION_DATA,
            Property.REQUEST_PROBLEM_INFORMATION,
            Property.REQUEST_RESPONSE_INFORMATION,
            Property.RECEIVE_MAXIMUM,
            Property.TOPIC_ALIAS_MAXIMUM,
            Property.USER_PROPERTY,
            Property.MAXIMUM_PACKET_SIZE
        )

        private val validWillProperties = listOf(
            Property.PAYLOAD_FORMAT_INDICATOR,
            Property.MESSAGE_EXPIRY_INTERVAL,
            Property.CONTENT_TYPE,
            Property.RESPONSE_TOPIC,
            Property.CORRELATION_DATA,
            Property.WILL_DELAY_INTERVAL,
            Property.USER_PROPERTY
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
                throw MalformedPacketException(ReasonCode.MALFORMED_PACKET)
            val willFlag = ((byte shl 2) and 1) == 1
            val willQos = ((byte shl 4) and 1) or ((byte shl 3) and 1)
            val willRetain = ((byte shl 5) and 1) == 1
            if (willFlag) {
                if (willQos == 3)
                    throw MalformedPacketException(ReasonCode.MALFORMED_PACKET)
            } else {
                if (willQos != 0)
                    throw MalformedPacketException(ReasonCode.MALFORMED_PACKET)
                if (willRetain)
                    throw MalformedPacketException(ReasonCode.MALFORMED_PACKET)
            }

            return ConnectFlags(
                ((byte shl 7) and 1) == 1,
                ((byte shl 6) and 1) == 1,
                willRetain,
                willQos,
                willFlag,
                ((byte shl 1) and 1) == 1,
                reserved
            )
        }

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTConnect {
            checkFlags(flags)

            val inStream = ByteArrayInputStream(data)
            val protocolName = inStream.readUTF8String()
            if (protocolName != "MQTT")
                throw MalformedPacketException(ReasonCode.UNSUPPORTED_PROTOCOL_VERSION)
            val protocolVersion = inStream.read()
            if (protocolVersion != 5)
                throw MalformedPacketException(ReasonCode.UNSUPPORTED_PROTOCOL_VERSION)

            val connectFlags = connectFlags(inStream.read())
            val keepAlive = inStream.read2BytesInt()

            val properties = inStream.deserializeProperties(validProperties)

            // Payload
            val clientID = inStream.readUTF8String()

            val willProperties =
                if (connectFlags.willFlag) inStream.deserializeProperties(validWillProperties) else null
            val willTopic = if (connectFlags.willFlag) inStream.readUTF8String() else null
            val willPayload = if (connectFlags.willFlag) inStream.readBinaryData() else null
            val userName = if (connectFlags.userNameFlag) inStream.readUTF8String() else null
            val password = if (connectFlags.passwordFlag) inStream.readBinaryData() else null

            return MQTTConnect(
                protocolName,
                protocolVersion,
                connectFlags,
                keepAlive,
                properties,
                clientID,
                willProperties,
                willTopic,
                willPayload,
                userName,
                password
            )
        }

    }
}
