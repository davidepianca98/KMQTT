package mqtt.packets

import mqtt.MalformedPacketException
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
            throw MalformedPacketException(ReasonCodes.MALFORMED_PACKET)
    }

    fun ByteArrayInputStream.getPacketIdentifier(): Int {
        return (this.read() shl 1) or this.read()
    }

    fun Int.flagsBit(bit: Int): Int {
        require(bit in 0..3)
        return (this shr bit) and 0b1
    }

    fun ByteArrayInputStream.read4BytesInt(): Int {
        return (read() shl 24) or (read() shl 16) or (read() shl 8) or read()
    }

    fun ByteArrayInputStream.read2BytesInt(): Int {
        return (read() shl 8) or read()
    }

    fun ByteArrayInputStream.readUTF8String(): String {
        val length = read2BytesInt()
        return String(readNBytes(length), StandardCharsets.UTF_8)
    }

    fun ByteArrayInputStream.readBinaryData(): ByteArray {
        val length = read2BytesInt()
        return readNBytes(length)
    }

    fun ByteArrayInputStream.readUTF8StringPair(): Pair<String, String> {
        return Pair(readUTF8String(), readUTF8String())
    }

    fun ByteArrayInputStream.deserializeProperties(validProperties: List<Property>): MQTTProperties {
        val propertyLength = decodeVariableByteInteger()

        val properties = MQTTProperties()
        for (i in 0 until propertyLength) {
            val propertyId = Property.valueOf(decodeVariableByteInteger())
            if (propertyId !in validProperties)
                throw IllegalArgumentException()
            when (propertyId) { // TODO check for duplicates of certain properties that are not allowed
                Property.PAYLOAD_FORMAT_INDICATOR -> properties.payloadFormatIndicator = read()
                Property.MESSAGE_EXPIRY_INTERVAL -> properties.messageExpiryInterval = read4BytesInt()
                Property.CONTENT_TYPE -> properties.contentType = readUTF8String()
                Property.RESPONSE_TOPIC -> properties.responseTopic = readUTF8String()
                Property.CORRELATION_DATA -> properties.correlationData = readBinaryData()
                Property.SUBSCRIPTION_IDENTIFIER -> properties.subscriptionIdentifier = decodeVariableByteInteger()
                Property.SESSION_EXPIRY_INTERVAL -> properties.sessionExpiryInterval = read4BytesInt()
                Property.ASSIGNED_CLIENT_IDENTIFIER -> properties.assignedClientIdentifier = readUTF8String()
                Property.SERVER_KEEP_ALIVE -> properties.serverKeepAlive = read2BytesInt()
                Property.AUTHENTICATION_METHOD -> properties.authenticationMethod = readUTF8String()
                Property.AUTHENTICATION_DATA -> properties.authenticationData = readBinaryData()
                Property.REQUEST_PROBLEM_INFORMATION -> properties.requestProblemInformation = read()
                Property.WILL_DELAY_INTERVAL -> properties.willDelayInterval = read4BytesInt()
                Property.REQUEST_RESPONSE_INFORMATION -> properties.requestResponseInformation = read()
                Property.RESPONSE_INFORMATION -> properties.responseInformation = readUTF8String()
                Property.SERVER_REFERENCE -> properties.serverReference = readUTF8String()
                Property.REASON_STRING -> properties.reasonString = readUTF8String()
                Property.RECEIVE_MAXIMUM -> properties.receiveMaximum = read2BytesInt()
                Property.TOPIC_ALIAS_MAXIMUM -> properties.topicAliasMaximum = read2BytesInt()
                Property.TOPIC_ALIAS -> properties.topicAlias = read2BytesInt()
                Property.MAXIMUM_QOS -> properties.maximumQos = read()
                Property.RETAIN_AVAILABLE -> properties.retainAvailable = read()
                Property.USER_PROPERTY -> properties.addUserProperty(readUTF8StringPair())
                Property.MAXIMUM_PACKET_SIZE -> properties.maximumPacketSize = read4BytesInt()
                Property.WILDCARD_SUBSCRIPTION_AVAILABLE -> properties.wildcardSubscriptionAvailable = read()
                Property.SUBSCRIPTION_IDENTIFIER_AVAILABLE -> properties.subscriptionIdentifierAvailable = read()
                Property.SHARED_SUBSCRIPTION_AVAILABLE -> properties.sharedSubscriptionAvailable = read()
                null -> throw MalformedPacketException(ReasonCodes.MALFORMED_PACKET)
            }
        }
        return properties
    }
}
