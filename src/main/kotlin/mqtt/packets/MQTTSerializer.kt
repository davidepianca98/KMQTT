package mqtt.packets

import mqtt.encodeVariableByteInteger
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

interface MQTTSerializer {

    fun toByteArray(): ByteArray

    fun ByteArrayOutputStream.write4BytesInt(value: UInt) {
        val byteArray = ByteArray(4)
        byteArray[0] = ((value shr 24) and 0xFFu).toByte()
        byteArray[1] = ((value shr 16) and 0xFFu).toByte()
        byteArray[2] = ((value shr 8) and 0xFFu).toByte()
        byteArray[3] = (value and 0xFFu).toByte()
        write(byteArray)
    }

    fun ByteArrayOutputStream.write2BytesInt(value: UInt) {
        val byteArray = ByteArray(2)
        byteArray[0] = ((value shr 8) and 0xFFu).toByte()
        byteArray[1] = (value and 0xFFu).toByte()
        write(byteArray)
    }

    fun ByteArrayOutputStream.writeByte(value: UInt) {
        write(value.toInt())
    }

    fun ByteArrayOutputStream.writeUTF8String(value: String) {
        write2BytesInt(value.length.toUInt())
        write(value.toByteArray(StandardCharsets.UTF_8))
    }

    fun ByteArrayOutputStream.writeBinaryData(data: ByteArray) {
        write2BytesInt(data.size.toUInt())
        writeBytes(data)
    }

    fun ByteArrayOutputStream.writeUTF8StringPair(value: Pair<String, String>) {
        writeUTF8String(value.first)
        writeUTF8String(value.second)
    }

    fun MQTTProperties.serializeProperties(validProperties: List<Property>): ByteArray {
        val out = ByteArrayOutputStream()
        payloadFormatIndicator?.let {
            if (Property.PAYLOAD_FORMAT_INDICATOR in validProperties) {
                out.write(Property.PAYLOAD_FORMAT_INDICATOR.ordinal)
                out.writeByte(it)
            }
        }

        messageExpiryInterval?.let {
            if (Property.MESSAGE_EXPIRY_INTERVAL in validProperties) {
                out.write(Property.MESSAGE_EXPIRY_INTERVAL.ordinal)
                out.write4BytesInt(it)
            }
        }

        contentType?.let {
            if (Property.CONTENT_TYPE in validProperties) {
                out.write(Property.CONTENT_TYPE.ordinal)
                out.writeUTF8String(it)
            }
        }

        responseTopic?.let {
            if (Property.RESPONSE_TOPIC in validProperties) {
                out.write(Property.RESPONSE_TOPIC.ordinal)
                out.writeUTF8String(it)
            }
        }

        correlationData?.let {
            if (Property.CORRELATION_DATA in validProperties) {
                out.write(Property.CORRELATION_DATA.ordinal)
                out.writeBinaryData(it)
            }
        }

        subscriptionIdentifier.forEach {
            if (Property.SUBSCRIPTION_IDENTIFIER in validProperties) {
                out.write(Property.SUBSCRIPTION_IDENTIFIER.ordinal)
                out.encodeVariableByteInteger(it)
            }
        }

        sessionExpiryInterval?.let {
            if (Property.SESSION_EXPIRY_INTERVAL in validProperties) {
                out.write(Property.SESSION_EXPIRY_INTERVAL.ordinal)
                out.write4BytesInt(it)
            }
        }

        assignedClientIdentifier?.let {
            if (Property.ASSIGNED_CLIENT_IDENTIFIER in validProperties) {
                out.write(Property.ASSIGNED_CLIENT_IDENTIFIER.ordinal)
                out.writeUTF8String(it)
            }
        }

        serverKeepAlive?.let {
            if (Property.SERVER_KEEP_ALIVE in validProperties) {
                out.write(Property.SERVER_KEEP_ALIVE.ordinal)
                out.write2BytesInt(it)
            }
        }

        authenticationMethod?.let {
            if (Property.AUTHENTICATION_METHOD in validProperties) {
                out.write(Property.AUTHENTICATION_METHOD.ordinal)
                out.writeUTF8String(it)
            }
        }

        authenticationData?.let {
            if (Property.AUTHENTICATION_DATA in validProperties) {
                out.write(Property.AUTHENTICATION_DATA.ordinal)
                out.writeBinaryData(it)
            }
        }

        requestProblemInformation?.let {
            if (Property.REQUEST_PROBLEM_INFORMATION in validProperties) {
                out.write(Property.REQUEST_PROBLEM_INFORMATION.ordinal)
                out.writeByte(it)
            }
        }

        willDelayInterval?.let {
            if (Property.WILL_DELAY_INTERVAL in validProperties) {
                out.write(Property.WILL_DELAY_INTERVAL.ordinal)
                out.write4BytesInt(it)
            }
        }

        requestResponseInformation?.let {
            if (Property.REQUEST_RESPONSE_INFORMATION in validProperties) {
                out.write(Property.REQUEST_RESPONSE_INFORMATION.ordinal)
                out.writeByte(it)
            }
        }

        responseInformation?.let {
            if (Property.RESPONSE_INFORMATION in validProperties) {
                out.write(Property.RESPONSE_INFORMATION.ordinal)
                out.writeUTF8String(it)
            }
        }

        serverReference?.let {
            if (Property.SERVER_REFERENCE in validProperties) {
                out.write(Property.SERVER_REFERENCE.ordinal)
                out.writeUTF8String(it)
            }
        }

        reasonString?.let {
            if (Property.REASON_STRING in validProperties) {
                out.write(Property.REASON_STRING.ordinal)
                out.writeUTF8String(it)
            }
        }

        receiveMaximum?.let {
            if (Property.RECEIVE_MAXIMUM in validProperties) {
                out.write(Property.RECEIVE_MAXIMUM.ordinal)
                out.write2BytesInt(it)
            }
        }

        topicAliasMaximum?.let {
            if (Property.TOPIC_ALIAS_MAXIMUM in validProperties) {
                out.write(Property.TOPIC_ALIAS_MAXIMUM.ordinal)
                out.write2BytesInt(it)
            }
        }

        topicAlias?.let {
            if (Property.TOPIC_ALIAS in validProperties) {
                out.write(Property.TOPIC_ALIAS.ordinal)
                out.write2BytesInt(it)
            }
        }

        maximumQos?.let {
            if (Property.MAXIMUM_QOS in validProperties) {
                out.write(Property.MAXIMUM_QOS.ordinal)
                out.writeByte(it)
            }
        }

        retainAvailable?.let {
            if (Property.RETAIN_AVAILABLE in validProperties) {
                out.write(Property.RETAIN_AVAILABLE.ordinal)
                out.writeByte(it)
            }
        }

        userProperty.forEach {
            if (Property.USER_PROPERTY in validProperties) {
                out.write(Property.USER_PROPERTY.ordinal)
                out.writeUTF8StringPair(it.toPair())
            }
        }

        maximumPacketSize?.let {
            if (Property.MAXIMUM_PACKET_SIZE in validProperties) {
                out.write(Property.MAXIMUM_PACKET_SIZE.ordinal)
                out.write4BytesInt(it)
            }
        }

        wildcardSubscriptionAvailable?.let {
            if (Property.WILDCARD_SUBSCRIPTION_AVAILABLE in validProperties) {
                out.write(Property.WILDCARD_SUBSCRIPTION_AVAILABLE.ordinal)
                out.writeByte(it)
            }
        }

        subscriptionIdentifierAvailable?.let {
            if (Property.SUBSCRIPTION_IDENTIFIER_AVAILABLE in validProperties) {
                out.write(Property.SUBSCRIPTION_IDENTIFIER_AVAILABLE.ordinal)
                out.writeByte(it)
            }
        }

        sharedSubscriptionAvailable?.let {
            if (Property.SHARED_SUBSCRIPTION_AVAILABLE in validProperties) {
                out.write(Property.SHARED_SUBSCRIPTION_AVAILABLE.ordinal)
                out.writeByte(it)
            }
        }

        val result = ByteArrayOutputStream()
        result.encodeVariableByteInteger(out.size().toUInt())
        result.writeBytes(out.toByteArray())

        return result.toByteArray()
    }
}
