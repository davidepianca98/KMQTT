package mqtt.packets.mqtt

import currentTimeMillis
import mqtt.packets.MQTTControlPacketType
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos
import mqtt.packets.mqttv4.MQTT4Publish
import mqtt.packets.mqttv5.MQTT5Publish
import socket.streams.ByteArrayInputStream
import socket.streams.decodeVariableByteInteger

public abstract class MQTTPublish(
    public val retain: Boolean,
    public val qos: Qos = Qos.AT_MOST_ONCE,
    public val dup: Boolean = false,
    public val topicName: String,
    public val packetId: UInt?,
    public val payload: UByteArray? = null,
    public val timestamp: Long = currentTimeMillis()
) : MQTTPacket {
    public open fun messageExpiryIntervalExpired(): Boolean {
        return false
    }

    public open fun updateMessageExpiryInterval() {

    }

    public abstract fun setDuplicate(): MQTTPublish

    public companion object {
        public fun fromByteArray(mqttVersion: Int, data: UByteArray): MQTTPublish {
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
