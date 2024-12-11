package io.github.davidepianca98.mqtt

import io.github.davidepianca98.mqtt.packets.MQTTControlPacketType
import io.github.davidepianca98.mqtt.packets.MQTTDeserializer
import io.github.davidepianca98.mqtt.packets.MQTTPacket
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Connack
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Connect
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Disconnect
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Pingreq
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Pingresp
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Puback
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Pubcomp
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Publish
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Pubrec
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Pubrel
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Suback
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Subscribe
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Unsuback
import io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Unsubscribe
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Auth
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Connack
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Connect
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Disconnect
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Pingreq
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Pingresp
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Puback
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Pubcomp
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Publish
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Pubrec
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Pubrel
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Suback
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Subscribe
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Unsuback
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Unsubscribe
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.socket.streams.DynamicByteBuffer
import io.github.davidepianca98.socket.streams.EOFException
import io.github.davidepianca98.socket.streams.decodeVariableByteInteger
import kotlin.collections.plusAssign

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
        } catch (_: EOFException) {
            currentReceivedData.clearReadCounter()
        }
        return packets
    }

    private fun readPacket(): MQTTPacket {
        val byte1 = currentReceivedData.read()
        val mqttControlPacketType = (byte1.toInt() shr 4) and 0b1111
        val flags = byte1 and 0b1111u

        val type =
            MQTTControlPacketType.Companion.valueOf(mqttControlPacketType) ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)

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
                } catch (_: MQTTException) {
                    mqttVersion = MQTTVersion.MQTT3_1_1
                    parseMQTT4Packet(type, flags, data)
                }
            }
        }
    }

}
