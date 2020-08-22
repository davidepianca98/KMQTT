package mqtt.packets.mqttv5

import mqtt.MQTTException
import mqtt.packets.ConnectFlags
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.mqtt.MQTTConnect
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream

class MQTT5Connect(
    protocolName: String,
    protocolVersion: Int,
    connectFlags: ConnectFlags,
    keepAlive: Int,
    clientID: String = "",
    val properties: MQTT5Properties = MQTT5Properties(),
    val willProperties: MQTT5Properties? = null,
    willTopic: String? = null,
    willPayload: UByteArray? = null,
    userName: String? = null,
    password: UByteArray? = null
) : MQTTConnect(
    protocolName,
    protocolVersion,
    connectFlags,
    keepAlive,
    clientID,
    willTopic,
    willPayload,
    userName,
    password
) {

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

        override fun fromByteArray(flags: Int, data: UByteArray): MQTT5Connect {
            checkFlags(flags)

            val inStream = ByteArrayInputStream(data)
            val protocolName = inStream.readUTF8String()
            if (protocolName != "MQTT")
                throw MQTTException(ReasonCode.UNSUPPORTED_PROTOCOL_VERSION)
            val protocolVersion = inStream.read().toInt()
            if (protocolVersion != 5)
                throw MQTTException(ReasonCode.UNSUPPORTED_PROTOCOL_VERSION)

            val connectFlags = ConnectFlags.connectFlags(inStream.read().toInt())
            val keepAlive = inStream.read2BytesInt()

            val properties = inStream.deserializeProperties(validProperties)
            if (properties.authenticationData != null && properties.authenticationMethod == null)
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)

            // Payload
            val clientID = inStream.readUTF8String()

            val willProperties =
                if (connectFlags.willFlag) inStream.deserializeProperties(validWillProperties) else null
            val willTopic = if (connectFlags.willFlag) inStream.readUTF8String() else null
            val willPayload = if (connectFlags.willFlag) inStream.readBinaryData() else null
            val userName = if (connectFlags.userNameFlag) inStream.readUTF8String() else null
            val password = if (connectFlags.passwordFlag) inStream.readBinaryData() else null

            return MQTT5Connect(
                protocolName,
                protocolVersion,
                connectFlags,
                keepAlive.toInt(),
                clientID,
                properties,
                willProperties,
                willTopic,
                willPayload,
                userName,
                password
            )
        }
    }

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()
        outStream.writeUTF8String("MQTT")
        outStream.writeByte(5u)
        outStream.writeByte(connectFlags.toByte())
        outStream.write2BytesInt(keepAlive.toUInt())
        outStream.write(properties.serializeProperties(validProperties))

        // Payload
        outStream.writeUTF8String(clientID)
        try {
            if (connectFlags.willFlag) {
                outStream.write(willProperties!!.serializeProperties(validWillProperties))
                outStream.writeUTF8String(willTopic!!)
                outStream.writeBinaryData(willPayload!!)
            }
        } catch (e: NullPointerException) {
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
        }

        try {
            if (connectFlags.userNameFlag) {
                outStream.writeUTF8String(userName!!)
            }
        } catch (e: NullPointerException) {
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
        }

        try {
            if (connectFlags.passwordFlag) {
                outStream.writeBinaryData(password!!)
            }
        } catch (e: NullPointerException) {
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
        }

        return outStream.wrapWithFixedHeader(MQTTControlPacketType.CONNECT, 0)
    }
}
