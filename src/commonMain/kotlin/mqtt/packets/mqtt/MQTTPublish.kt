package mqtt.packets.mqtt

import currentTimeMillis
import mqtt.packets.MQTTPacket
import mqtt.packets.Qos

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
}
