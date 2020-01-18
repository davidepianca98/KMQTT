package mqtt.packets

import mqtt.MQTTControlPacketType
import mqtt.encodeVariableByteInteger
import java.io.ByteArrayOutputStream

data class ConnectAcknowledgeFlags(val sessionPresentFlag: Boolean)

data class MQTTConnack(
    val connectAcknowledgeFlags: ConnectAcknowledgeFlags,
    val connectReasonCode: ReasonCodes,
    val properties: MQTTProperties? = null
) : MQTTPacket, MQTTSerializer {

    private val validProperties = listOf(
        Property.SESSION_EXPIRY_INTERVAL,
        Property.ASSIGNED_CLIENT_IDENTIFIER,
        Property.SERVER_KEEP_ALIVE,
        Property.AUTHENTICATION_METHOD,
        Property.AUTHENTICATION_DATA,
        Property.RESPONSE_INFORMATION,
        Property.SERVER_REFERENCE,
        Property.REASON_STRING,
        Property.RECEIVE_MAXIMUM,
        Property.TOPIC_ALIAS_MAXIMUM,
        Property.MAXIMUM_QOS,
        Property.RETAIN_AVAILABLE,
        Property.USER_PROPERTY,
        Property.MAXIMUM_PACKET_SIZE,
        Property.WILDCARD_SUBSCRIPTION_AVAILABLE,
        Property.SUBSCRIPTION_IDENTIFIER_AVAILABLE,
        Property.SHARED_SUBSCRIPTION_AVAILABLE
    )

    override fun toByteArray(): ByteArray {
        val outStream = ByteArrayOutputStream()

        // TODO if nonzero reason code, session present = 0
        outStream.write(if (connectAcknowledgeFlags.sessionPresentFlag) 1 else 0)
        outStream.write(connectReasonCode.ordinal)
        properties?.let {
            outStream.writeBytes(it.serializeProperties(validProperties))
        } ?: run {
            outStream.encodeVariableByteInteger(0)
        }

        val result = ByteArrayOutputStream()
        result.write((MQTTControlPacketType.CONNACK.ordinal shl 4) and 0xF0)
        result.encodeVariableByteInteger(outStream.size())
        return result.toByteArray()
    }
}
