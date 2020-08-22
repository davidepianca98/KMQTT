package mqtt

import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTPacket
import mqtt.packets.mqttv4.*
import mqtt.packets.mqttv5.*
import socket.streams.DynamicByteBuffer
import socket.streams.EOFException
import socket.streams.decodeVariableByteInteger

class MQTTCurrentPacket(private val maximumPacketSize: UInt) {

    private val currentReceivedData = DynamicByteBuffer()
    internal var mqttVersion: Int? = null
        private set

    fun addData(data: UByteArray): List<MQTTPacket> {
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

        val type = MQTTControlPacketType.valueOf(mqttControlPacketType)!!

        val remainingLength = currentReceivedData.decodeVariableByteInteger().toInt()

        if (remainingLength + 2 > maximumPacketSize.toInt())
            throw MQTTException(ReasonCode.PACKET_TOO_LARGE)

        val packetData = currentReceivedData.readBytes(remainingLength)
        val packet = parseMQTTPacket(type, flags.toInt(), packetData)
        currentReceivedData.shift()
        return packet
    }

    private fun parseMQTTPacket(type: MQTTControlPacketType, flags: Int, data: UByteArray): MQTTPacket {
        return when (type) {
            MQTTControlPacketType.CONNECT -> {
                when (mqttVersion) {
                    4 -> MQTT4Connect.fromByteArray(flags, data)
                    5 -> MQTT5Connect.fromByteArray(flags, data)
                    else -> {
                        try {
                            mqttVersion = 5
                            MQTT5Connect.fromByteArray(flags, data)
                        } catch (e: MQTTException) {
                            mqttVersion = 4
                            MQTT4Connect.fromByteArray(flags, data)
                        }
                    }
                }

            }
            MQTTControlPacketType.Reserved -> throw MQTTException(ReasonCode.MALFORMED_PACKET)
            MQTTControlPacketType.CONNACK -> if (mqttVersion == 4) MQTT4Connack.fromByteArray(
                flags,
                data
            ) else MQTT5Connack.fromByteArray(
                flags,
                data
            )
            MQTTControlPacketType.PUBLISH -> if (mqttVersion == 4) MQTT4Publish.fromByteArray(
                flags,
                data
            ) else MQTT5Publish.fromByteArray(flags, data)
            MQTTControlPacketType.PUBACK -> if (mqttVersion == 4) MQTT4Puback.fromByteArray(
                flags,
                data
            ) else MQTT5Puback.fromByteArray(flags, data)
            MQTTControlPacketType.PUBREC -> if (mqttVersion == 4) MQTT4Pubrec.fromByteArray(
                flags,
                data
            ) else MQTT5Pubrec.fromByteArray(flags, data)
            MQTTControlPacketType.PUBREL -> if (mqttVersion == 4) MQTT4Pubrel.fromByteArray(
                flags,
                data
            ) else MQTT5Pubrel.fromByteArray(flags, data)
            MQTTControlPacketType.PUBCOMP -> if (mqttVersion == 4) MQTT4Pubcomp.fromByteArray(
                flags,
                data
            ) else MQTT5Pubcomp.fromByteArray(flags, data)
            MQTTControlPacketType.SUBSCRIBE -> if (mqttVersion == 4) MQTT4Subscribe.fromByteArray(
                flags,
                data
            ) else MQTT5Subscribe.fromByteArray(flags, data)
            MQTTControlPacketType.SUBACK -> if (mqttVersion == 4) MQTT4Suback.fromByteArray(
                flags,
                data
            ) else MQTT5Suback.fromByteArray(flags, data)
            MQTTControlPacketType.UNSUBSCRIBE -> if (mqttVersion == 4) MQTT4Unsubscribe.fromByteArray(
                flags,
                data
            ) else MQTT5Unsubscribe.fromByteArray(flags, data)
            MQTTControlPacketType.UNSUBACK -> if (mqttVersion == 4) MQTT4Unsuback.fromByteArray(
                flags,
                data
            ) else MQTT5Unsuback.fromByteArray(flags, data)
            MQTTControlPacketType.PINGREQ -> if (mqttVersion == 4) MQTT4Pingreq.fromByteArray(
                flags,
                data
            ) else MQTT5Pingreq.fromByteArray(flags, data)
            MQTTControlPacketType.PINGRESP -> if (mqttVersion == 4) MQTT4Pingresp.fromByteArray(
                flags,
                data
            ) else MQTT5Pingresp.fromByteArray(flags, data)
            MQTTControlPacketType.DISCONNECT -> if (mqttVersion == 4) MQTT4Disconnect.fromByteArray(
                flags,
                data
            ) else MQTT5Disconnect.fromByteArray(flags, data)
            MQTTControlPacketType.AUTH -> if (mqttVersion == 4) throw MQTTException(ReasonCode.PROTOCOL_ERROR) else MQTT5Auth.fromByteArray(
                flags,
                data
            )
        }
    }

}
