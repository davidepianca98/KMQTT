package mqtt.packets.mqtt

import currentTimeMillis
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqttv4.MQTT4Publish
import mqtt.packets.mqttv5.MQTT5Publish
import socket.streams.ByteArrayInputStream
import socket.streams.decodeVariableByteInteger

abstract class MQTTPublish(
    val retain: Boolean,
    val qos: Qos = Qos.AT_MOST_ONCE,
    val dup: Boolean = false,
    val topicName: String,
    val packetId: UInt?,
    val payload: UByteArray? = null,
    val timestamp: Long = currentTimeMillis()
) : MQTTPacket {
    open fun messageExpiryIntervalExpired(): Boolean {
        return false
    }

    open fun updateMessageExpiryInterval() {

    }

    abstract fun setDuplicate(): MQTTPublish

    companion object {
        fun fromByteArray(mqttVersion: Int, data: UByteArray): MQTTPublish {
            val inStream = ByteArrayInputStream(data)
            val byte1 = inStream.read()
            val mqttControlPacketType = (byte1.toInt() shr 4) and 0b1111
            val flags = byte1 and 0b1111u

            @Suppress("UNUSED_VARIABLE")
            val type = MQTTControlPacketType.valueOf(mqttControlPacketType)

            @Suppress("UNUSED_VARIABLE")
            val remainingLength = inStream.decodeVariableByteInteger().toInt()

            return if (mqttVersion == 5) {
                MQTT5Publish.fromByteArray(flags.toInt(), data.copyOfRange(data.size - inStream.available(), data.size))
            } else {
                MQTT4Publish.fromByteArray(flags.toInt(), data.copyOfRange(data.size - inStream.available(), data.size))
            }
        }
    }
}
