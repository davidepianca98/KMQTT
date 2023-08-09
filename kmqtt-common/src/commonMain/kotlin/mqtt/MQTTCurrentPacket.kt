package mqtt

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTDeserializer
import mqtt.packets.MQTTPacket
import mqtt.packets.mqttv4.*
import mqtt.packets.mqttv5.*
import socket.streams.DynamicByteBuffer
import socket.streams.EOFException
import socket.streams.decodeVariableByteInteger

public class MQTTCurrentPacket(
    private val maximumPacketSize: UInt,
    mqttVersion: MQTTVersion? = null // null -> unknown (support both)
) {

    private val currentReceivedData = DynamicByteBuffer()
    public var mqttVersion: MQTTVersion? = mqttVersion
        private set

    public fun addData(data: UByteArray): List<MQTTPacket> {
        val packets = mutableListOf<MQTTPacket>()
        currentReceivedData.write(data)
        try {
            while (true) {
                packets += readPacket()
            }
        } catch (e: EOFException) {
            currentReceivedData.clearReadCounter()
        }
        return packets
    }

    private fun readPacket(): MQTTPacket {
        val byte1 = currentReceivedData.read()
        val mqttControlPacketType = (byte1.toInt() shr 4) and 0b1111
        val flags = byte1 and 0b1111u

        val type =
            MQTTControlPacketType.valueOf(mqttControlPacketType) ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)

        val remainingLength = currentReceivedData.decodeVariableByteInteger().toInt()

        if (remainingLength + 2 > maximumPacketSize.toInt())
            throw MQTTException(ReasonCode.PACKET_TOO_LARGE)

        val packetData = currentReceivedData.readBytes(remainingLength)
        val packet = parseMQTTPacket(type, flags.toInt(), packetData)
        currentReceivedData.shift()
        return packet
    }

    private fun parse(deserializer: MQTTDeserializer, flags: Int, data: UByteArray): MQTTPacket {
        return deserializer.fromByteArray(flags, data)
    }

    private fun parseMQTT4Packet(type: MQTTControlPacketType, flags: Int, data: UByteArray): MQTTPacket {
        return when (type) {
            MQTTControlPacketType.CONNECT -> parse(MQTT4Connect, flags, data)
            MQTTControlPacketType.Reserved -> throw MQTTException(ReasonCode.MALFORMED_PACKET)
            MQTTControlPacketType.CONNACK -> parse(MQTT4Connack, flags, data)
            MQTTControlPacketType.PUBLISH -> parse(MQTT4Publish, flags, data)
            MQTTControlPacketType.PUBACK -> parse(MQTT4Puback, flags, data)
            MQTTControlPacketType.PUBREC -> parse(MQTT4Pubrec, flags, data)
            MQTTControlPacketType.PUBREL -> parse(MQTT4Pubrel, flags, data)
            MQTTControlPacketType.PUBCOMP -> parse(MQTT4Pubcomp, flags, data)
            MQTTControlPacketType.SUBSCRIBE -> parse(MQTT4Subscribe, flags, data)
            MQTTControlPacketType.SUBACK -> parse(MQTT4Suback, flags, data)
            MQTTControlPacketType.UNSUBSCRIBE -> parse(MQTT4Unsubscribe, flags, data)
            MQTTControlPacketType.UNSUBACK -> parse(MQTT4Unsuback, flags, data)
            MQTTControlPacketType.PINGREQ -> parse(MQTT4Pingreq, flags, data)
            MQTTControlPacketType.PINGRESP -> parse(MQTT4Pingresp, flags, data)
            MQTTControlPacketType.DISCONNECT -> parse(MQTT4Disconnect, flags, data)
            MQTTControlPacketType.AUTH -> throw MQTTException(ReasonCode.PROTOCOL_ERROR)
        }
    }

    private fun parseMQTT5Packet(type: MQTTControlPacketType, flags: Int, data: UByteArray): MQTTPacket {
        return when (type) {
            MQTTControlPacketType.CONNECT -> parse(MQTT5Connect, flags, data)
            MQTTControlPacketType.Reserved -> throw MQTTException(ReasonCode.MALFORMED_PACKET)
            MQTTControlPacketType.CONNACK -> parse(MQTT5Connack, flags, data)
            MQTTControlPacketType.PUBLISH -> parse(MQTT5Publish, flags, data)
            MQTTControlPacketType.PUBACK -> parse(MQTT5Puback, flags, data)
            MQTTControlPacketType.PUBREC -> parse(MQTT5Pubrec, flags, data)
            MQTTControlPacketType.PUBREL -> parse(MQTT5Pubrel, flags, data)
            MQTTControlPacketType.PUBCOMP -> parse(MQTT5Pubcomp, flags, data)
            MQTTControlPacketType.SUBSCRIBE -> parse(MQTT5Subscribe, flags, data)
            MQTTControlPacketType.SUBACK -> parse(MQTT5Suback, flags, data)
            MQTTControlPacketType.UNSUBSCRIBE -> parse(MQTT5Unsubscribe, flags, data)
            MQTTControlPacketType.UNSUBACK -> parse(MQTT5Unsuback, flags, data)
            MQTTControlPacketType.PINGREQ -> parse(MQTT5Pingreq, flags, data)
            MQTTControlPacketType.PINGRESP -> parse(MQTT5Pingresp, flags, data)
            MQTTControlPacketType.DISCONNECT -> parse(MQTT5Disconnect, flags, data)
            MQTTControlPacketType.AUTH -> parse(MQTT5Auth, flags, data)
        }
    }

    private fun parseMQTTPacket(type: MQTTControlPacketType, flags: Int, data: UByteArray): MQTTPacket {
        return when (mqttVersion) {
            MQTTVersion.MQTT3_1_1 -> parseMQTT4Packet(type, flags, data)
            MQTTVersion.MQTT5 -> parseMQTT5Packet(type, flags, data)
            else -> {
                try {
                    mqttVersion = MQTTVersion.MQTT5
                    parseMQTT5Packet(type, flags, data)
                } catch (e: MQTTException) {
                    mqttVersion = MQTTVersion.MQTT3_1_1
                    parseMQTT4Packet(type, flags, data)
                }
            }
        }
    }

}
