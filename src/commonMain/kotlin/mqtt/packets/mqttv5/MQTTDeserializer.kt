package mqtt.packets.mqttv5

import mqtt.MQTTException
import mqtt.containsWildcard
import socket.streams.ByteArrayInputStream
import socket.streams.decodeVariableByteInteger
import validateUTF8String

interface MQTTDeserializer {

    fun fromByteArray(flags: Int, data: UByteArray): MQTTPacket

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
        val string = readBytes(length).toByteArray().decodeToString()
        if (!string.validateUTF8String())
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
        return string
    }

    fun ByteArrayInputStream.readBinaryData(): UByteArray {
        val length = read2BytesInt().toInt()
        return readBytes(length)
    }

    fun ByteArrayInputStream.readUTF8StringPair(): Pair<String, String> {
        return Pair(readUTF8String(), readUTF8String())
    }

    fun ByteArrayInputStream.deserializeProperties(validProperties: List<Property>): MQTTProperties {
        val propertyLength = decodeVariableByteInteger()
        val initialTotalRemainingLength = available()

        val properties = MQTTProperties()
        while (initialTotalRemainingLength - available() < propertyLength.toInt()) {
            val propertyIdByte = decodeVariableByteInteger()
            val propertyId = Property.valueOf(propertyIdByte)
            if (propertyId !in validProperties)
                throw IllegalArgumentException()
            when (propertyId) {
                Property.PAYLOAD_FORMAT_INDICATOR -> {
                    if (properties.payloadFormatIndicator != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.payloadFormatIndicator = readByte()
                }
                Property.MESSAGE_EXPIRY_INTERVAL -> {
                    if (properties.messageExpiryInterval != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.messageExpiryInterval = read4BytesInt()
                }
                Property.CONTENT_TYPE -> {
                    if (properties.contentType != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.contentType = readUTF8String()
                }
                Property.RESPONSE_TOPIC -> {
                    if (properties.responseTopic != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    val responseTopic = readUTF8String()
                    if (responseTopic.containsWildcard())
                        throw MQTTException(ReasonCode.TOPIC_NAME_INVALID)
                    properties.responseTopic = responseTopic
                }
                Property.CORRELATION_DATA -> {
                    if (properties.correlationData != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.correlationData = readBinaryData()
                }
                Property.SUBSCRIPTION_IDENTIFIER -> {
                    properties.subscriptionIdentifier.add(decodeVariableByteInteger())
                }
                Property.SESSION_EXPIRY_INTERVAL -> {
                    if (properties.sessionExpiryInterval != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.sessionExpiryInterval = read4BytesInt()
                }
                Property.ASSIGNED_CLIENT_IDENTIFIER -> {
                    if (properties.assignedClientIdentifier != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.assignedClientIdentifier = readUTF8String()
                }
                Property.SERVER_KEEP_ALIVE -> {
                    if (properties.serverKeepAlive != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.serverKeepAlive = read2BytesInt()
                }
                Property.AUTHENTICATION_METHOD -> {
                    if (properties.authenticationMethod != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.authenticationMethod = readUTF8String()
                }
                Property.AUTHENTICATION_DATA -> {
                    if (properties.authenticationData != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.authenticationData = readBinaryData()
                }
                Property.REQUEST_PROBLEM_INFORMATION -> {
                    if (properties.requestProblemInformation != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.requestProblemInformation = readByte()
                }
                Property.WILL_DELAY_INTERVAL -> {
                    if (properties.willDelayInterval != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.willDelayInterval = read4BytesInt()
                }
                Property.REQUEST_RESPONSE_INFORMATION -> {
                    if (properties.requestResponseInformation != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.requestResponseInformation = readByte()
                }
                Property.RESPONSE_INFORMATION -> {
                    if (properties.responseInformation != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.responseInformation = readUTF8String()
                }
                Property.SERVER_REFERENCE -> {
                    if (properties.serverReference != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.serverReference = readUTF8String()
                }
                Property.REASON_STRING -> {
                    if (properties.reasonString != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.reasonString = readUTF8String()
                }
                Property.RECEIVE_MAXIMUM -> {
                    if (properties.receiveMaximum != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.receiveMaximum = read2BytesInt()
                }
                Property.TOPIC_ALIAS_MAXIMUM -> {
                    if (properties.topicAliasMaximum != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.topicAliasMaximum = read2BytesInt()
                }
                Property.TOPIC_ALIAS -> {
                    if (properties.topicAlias != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.topicAlias = read2BytesInt()
                }
                Property.MAXIMUM_QOS -> {
                    if (properties.maximumQos != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.maximumQos = readByte()
                }
                Property.RETAIN_AVAILABLE -> {
                    if (properties.retainAvailable != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.retainAvailable = readByte()
                }
                Property.USER_PROPERTY -> {
                    properties.addUserProperty(readUTF8StringPair())
                }
                Property.MAXIMUM_PACKET_SIZE -> {
                    if (properties.maximumPacketSize != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.maximumPacketSize = read4BytesInt()
                }
                Property.WILDCARD_SUBSCRIPTION_AVAILABLE -> {
                    if (properties.wildcardSubscriptionAvailable != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.wildcardSubscriptionAvailable = readByte()
                }
                Property.SUBSCRIPTION_IDENTIFIER_AVAILABLE -> {
                    if (properties.subscriptionIdentifierAvailable != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.subscriptionIdentifierAvailable = readByte()
                }
                Property.SHARED_SUBSCRIPTION_AVAILABLE -> {
                    if (properties.sharedSubscriptionAvailable != null)
                        throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                    properties.sharedSubscriptionAvailable = readByte()
                }
                null -> throw MQTTException(ReasonCode.MALFORMED_PACKET)
            }
        }
        return properties
    }
}
