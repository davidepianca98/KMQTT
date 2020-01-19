package mqtt.packets

import mqtt.MQTTException
import mqtt.containsWildcard
import mqtt.decodeVariableByteInteger
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

interface MQTTDeserializer {

    fun fromByteArray(flags: Int, data: ByteArray): MQTTPacket

    fun checkFlags(flags: Int) {
        if (flags.flagsBit(0) != 0 ||
            flags.flagsBit(1) != 0 ||
            flags.flagsBit(2) != 0 ||
            flags.flagsBit(3) != 0
        )
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
    }

    fun Int.flagsBit(bit: Int): Int {
        require(bit in 0..3)
        return (this shr bit) and 0b1
    }

    fun ByteArrayInputStream.read4BytesInt(): UInt {
        return (read().toUInt() shl 24) or (read().toUInt() shl 16) or (read().toUInt() shl 8) or read().toUInt()
    }

    fun ByteArrayInputStream.read2BytesInt(): UInt {
        return (read().toUInt() shl 8) or read().toUInt()
    }

    fun ByteArrayInputStream.readByte(): UInt {
        return read().toUInt()
    }

    fun ByteArrayInputStream.readUTF8String(): String {
        val length = read2BytesInt().toInt()
        return String(readNBytes(length), StandardCharsets.UTF_8)
    }

    fun ByteArrayInputStream.readBinaryData(): ByteArray {
        val length = read2BytesInt().toInt()
        return readNBytes(length)
    }

    fun ByteArrayInputStream.readUTF8StringPair(): Pair<String, String> {
        return Pair(readUTF8String(), readUTF8String())
    }

    fun ByteArrayInputStream.deserializeProperties(validProperties: List<Property>): MQTTProperties {
        val propertyLength = decodeVariableByteInteger()

        val properties = MQTTProperties()
        for (i in 0 until propertyLength.toInt()) {
            val propertyId = Property.valueOf(decodeVariableByteInteger().toInt())
            if (propertyId !in validProperties)
                throw IllegalArgumentException()
            when (propertyId) { // TODO check for duplicates of certain properties that are not allowed
                Property.PAYLOAD_FORMAT_INDICATOR -> properties.payloadFormatIndicator = readByte()
                Property.MESSAGE_EXPIRY_INTERVAL -> properties.messageExpiryInterval = read4BytesInt()
                Property.CONTENT_TYPE -> properties.contentType = readUTF8String()
                Property.RESPONSE_TOPIC -> {
                    val responseTopic = readUTF8String()
                    if (responseTopic.containsWildcard())
                        throw MQTTException(ReasonCode.TOPIC_NAME_INVALID)
                    properties.responseTopic = responseTopic
                }
                Property.CORRELATION_DATA -> properties.correlationData = readBinaryData()
                Property.SUBSCRIPTION_IDENTIFIER -> properties.subscriptionIdentifier.add(decodeVariableByteInteger())
                Property.SESSION_EXPIRY_INTERVAL -> properties.sessionExpiryInterval = read4BytesInt()
                Property.ASSIGNED_CLIENT_IDENTIFIER -> properties.assignedClientIdentifier = readUTF8String()
                Property.SERVER_KEEP_ALIVE -> properties.serverKeepAlive = read2BytesInt()
                Property.AUTHENTICATION_METHOD -> properties.authenticationMethod = readUTF8String()
                Property.AUTHENTICATION_DATA -> properties.authenticationData = readBinaryData()
                Property.REQUEST_PROBLEM_INFORMATION -> properties.requestProblemInformation = readByte()
                Property.WILL_DELAY_INTERVAL -> properties.willDelayInterval = read4BytesInt()
                Property.REQUEST_RESPONSE_INFORMATION -> properties.requestResponseInformation = readByte()
                Property.RESPONSE_INFORMATION -> properties.responseInformation = readUTF8String()
                Property.SERVER_REFERENCE -> properties.serverReference = readUTF8String()
                Property.REASON_STRING -> properties.reasonString = readUTF8String()
                Property.RECEIVE_MAXIMUM -> properties.receiveMaximum = read2BytesInt()
                Property.TOPIC_ALIAS_MAXIMUM -> properties.topicAliasMaximum = read2BytesInt()
                Property.TOPIC_ALIAS -> properties.topicAlias = read2BytesInt()
                Property.MAXIMUM_QOS -> properties.maximumQos = readByte()
                Property.RETAIN_AVAILABLE -> properties.retainAvailable = readByte()
                Property.USER_PROPERTY -> properties.addUserProperty(readUTF8StringPair())
                Property.MAXIMUM_PACKET_SIZE -> properties.maximumPacketSize = read4BytesInt()
                Property.WILDCARD_SUBSCRIPTION_AVAILABLE -> properties.wildcardSubscriptionAvailable = readByte()
                Property.SUBSCRIPTION_IDENTIFIER_AVAILABLE -> properties.subscriptionIdentifierAvailable = readByte()
                Property.SHARED_SUBSCRIPTION_AVAILABLE -> properties.sharedSubscriptionAvailable = readByte()
                null -> throw MQTTException(ReasonCode.MALFORMED_PACKET)
            }
        }
        return properties
    }
}
