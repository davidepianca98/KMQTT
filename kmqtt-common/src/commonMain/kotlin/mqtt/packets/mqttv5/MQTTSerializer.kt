package mqtt.packets.mqttv5

import mqtt.MQTTException
import mqtt.packets.MQTTControlPacketType
import socket.streams.ByteArrayOutputStream
import socket.streams.encodeVariableByteInteger
import validateUTF8String

public interface MQTTSerializer {

    public fun toByteArray(): UByteArray

    public fun size(): UInt = toByteArray().size.toUInt()

    public fun ByteArrayOutputStream.wrapWithFixedHeader(packetType: MQTTControlPacketType, flags: Int): UByteArray {
        require(flags in 0..15)
        val result = ByteArrayOutputStream()
        val fixedHeader = ((packetType.value shl 4) and 0xF0) or flags
        result.write(fixedHeader.toUByte())
        result.encodeVariableByteInteger(this.size().toUInt())
        result.write(this.toByteArray())
        return result.toByteArray()
    }

    public fun ByteArrayOutputStream.write4BytesInt(value: UInt) {
        val byteArray = UByteArray(4)
        byteArray[0] = ((value shr 24) and 0xFFu).toUByte()
        byteArray[1] = ((value shr 16) and 0xFFu).toUByte()
        byteArray[2] = ((value shr 8) and 0xFFu).toUByte()
        byteArray[3] = (value and 0xFFu).toUByte()
        write(byteArray)
    }

    public fun ByteArrayOutputStream.write2BytesInt(value: UInt) {
        val byteArray = UByteArray(2)
        byteArray[0] = ((value shr 8) and 0xFFu).toUByte()
        byteArray[1] = (value and 0xFFu).toUByte()
        write(byteArray)
    }

    public fun ByteArrayOutputStream.writeByte(value: UInt) {
        write(value.toUByte())
    }

    public fun ByteArrayOutputStream.writeUTF8String(value: String) {
        if (!value.validateUTF8String())
            throw MQTTException(ReasonCode.MALFORMED_PACKET)
        write2BytesInt(value.length.toUInt())
        write(value.encodeToByteArray().toUByteArray())
    }

    public fun ByteArrayOutputStream.writeBinaryData(data: UByteArray) {
        write2BytesInt(data.size.toUInt())
        write(data)
    }

    public fun ByteArrayOutputStream.writeUTF8StringPair(value: Pair<String, String>) {
        writeUTF8String(value.first)
        writeUTF8String(value.second)
    }

    public fun MQTT5Properties.serializeProperties(validProperties: List<Property>): UByteArray {
        val out = ByteArrayOutputStream()
        payloadFormatIndicator?.let {
            if (Property.PAYLOAD_FORMAT_INDICATOR in validProperties) {
                out.write(Property.PAYLOAD_FORMAT_INDICATOR.value.toUByte())
                out.writeByte(it)
            }
        }

        messageExpiryInterval?.let {
            if (Property.MESSAGE_EXPIRY_INTERVAL in validProperties) {
                out.write(Property.MESSAGE_EXPIRY_INTERVAL.value.toUByte())
                out.write4BytesInt(it)
            }
        }

        contentType?.let {
            if (Property.CONTENT_TYPE in validProperties) {
                out.write(Property.CONTENT_TYPE.value.toUByte())
                out.writeUTF8String(it)
            }
        }

        responseTopic?.let {
            if (Property.RESPONSE_TOPIC in validProperties) {
                out.write(Property.RESPONSE_TOPIC.value.toUByte())
                out.writeUTF8String(it)
            }
        }

        correlationData?.let {
            if (Property.CORRELATION_DATA in validProperties) {
                out.write(Property.CORRELATION_DATA.value.toUByte())
                out.writeBinaryData(it)
            }
        }

        subscriptionIdentifier.forEach {
            if (Property.SUBSCRIPTION_IDENTIFIER in validProperties) {
                out.write(Property.SUBSCRIPTION_IDENTIFIER.value.toUByte())
                out.encodeVariableByteInteger(it)
            }
        }

        sessionExpiryInterval?.let {
            if (Property.SESSION_EXPIRY_INTERVAL in validProperties) {
                out.write(Property.SESSION_EXPIRY_INTERVAL.value.toUByte())
                out.write4BytesInt(it)
            }
        }

        assignedClientIdentifier?.let {
            if (Property.ASSIGNED_CLIENT_IDENTIFIER in validProperties) {
                out.write(Property.ASSIGNED_CLIENT_IDENTIFIER.value.toUByte())
                out.writeUTF8String(it)
            }
        }

        serverKeepAlive?.let {
            if (Property.SERVER_KEEP_ALIVE in validProperties) {
                out.write(Property.SERVER_KEEP_ALIVE.value.toUByte())
                out.write2BytesInt(it)
            }
        }

        authenticationMethod?.let {
            if (Property.AUTHENTICATION_METHOD in validProperties) {
                out.write(Property.AUTHENTICATION_METHOD.value.toUByte())
                out.writeUTF8String(it)
            }
        }

        authenticationData?.let {
            if (Property.AUTHENTICATION_DATA in validProperties) {
                out.write(Property.AUTHENTICATION_DATA.value.toUByte())
                out.writeBinaryData(it)
            }
        }

        requestProblemInformation?.let {
            if (Property.REQUEST_PROBLEM_INFORMATION in validProperties) {
                out.write(Property.REQUEST_PROBLEM_INFORMATION.value.toUByte())
                out.writeByte(it)
            }
        }

        willDelayInterval?.let {
            if (Property.WILL_DELAY_INTERVAL in validProperties) {
                out.write(Property.WILL_DELAY_INTERVAL.value.toUByte())
                out.write4BytesInt(it)
            }
        }

        requestResponseInformation?.let {
            if (Property.REQUEST_RESPONSE_INFORMATION in validProperties) {
                out.write(Property.REQUEST_RESPONSE_INFORMATION.value.toUByte())
                out.writeByte(it)
            }
        }

        responseInformation?.let {
            if (Property.RESPONSE_INFORMATION in validProperties) {
                out.write(Property.RESPONSE_INFORMATION.value.toUByte())
                out.writeUTF8String(it)
            }
        }

        serverReference?.let {
            if (Property.SERVER_REFERENCE in validProperties) {
                out.write(Property.SERVER_REFERENCE.value.toUByte())
                out.writeUTF8String(it)
            }
        }

        reasonString?.let {
            if (Property.REASON_STRING in validProperties) {
                out.write(Property.REASON_STRING.value.toUByte())
                out.writeUTF8String(it)
            }
        }

        receiveMaximum?.let {
            if (Property.RECEIVE_MAXIMUM in validProperties) {
                out.write(Property.RECEIVE_MAXIMUM.value.toUByte())
                out.write2BytesInt(it)
            }
        }

        topicAliasMaximum?.let {
            if (Property.TOPIC_ALIAS_MAXIMUM in validProperties) {
                out.write(Property.TOPIC_ALIAS_MAXIMUM.value.toUByte())
                out.write2BytesInt(it)
            }
        }

        topicAlias?.let {
            if (Property.TOPIC_ALIAS in validProperties) {
                out.write(Property.TOPIC_ALIAS.value.toUByte())
                out.write2BytesInt(it)
            }
        }

        maximumQos?.let {
            if (Property.MAXIMUM_QOS in validProperties) {
                out.write(Property.MAXIMUM_QOS.value.toUByte())
                out.writeByte(it)
            }
        }

        retainAvailable?.let {
            if (Property.RETAIN_AVAILABLE in validProperties) {
                out.write(Property.RETAIN_AVAILABLE.value.toUByte())
                out.writeByte(it)
            }
        }

        userProperty.forEach {
            if (Property.USER_PROPERTY in validProperties) {
                out.write(Property.USER_PROPERTY.value.toUByte())
                out.writeUTF8StringPair(it)
            }
        }

        maximumPacketSize?.let {
            if (Property.MAXIMUM_PACKET_SIZE in validProperties) {
                out.write(Property.MAXIMUM_PACKET_SIZE.value.toUByte())
                out.write4BytesInt(it)
            }
        }

        wildcardSubscriptionAvailable?.let {
            if (Property.WILDCARD_SUBSCRIPTION_AVAILABLE in validProperties) {
                out.write(Property.WILDCARD_SUBSCRIPTION_AVAILABLE.value.toUByte())
                out.writeByte(it)
            }
        }

        subscriptionIdentifierAvailable?.let {
            if (Property.SUBSCRIPTION_IDENTIFIER_AVAILABLE in validProperties) {
                out.write(Property.SUBSCRIPTION_IDENTIFIER_AVAILABLE.value.toUByte())
                out.writeByte(it)
            }
        }

        sharedSubscriptionAvailable?.let {
            if (Property.SHARED_SUBSCRIPTION_AVAILABLE in validProperties) {
                out.write(Property.SHARED_SUBSCRIPTION_AVAILABLE.value.toUByte())
                out.writeByte(it)
            }
        }

        val result = ByteArrayOutputStream()
        result.encodeVariableByteInteger(out.size().toUInt())
        result.write(out.toByteArray())

        return result.toByteArray()
    }
}
