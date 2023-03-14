package mqtt.broker

import currentTimeMillis
import mqtt.Will
import mqtt.packets.Qos
import mqtt.packets.mqttv5.MQTT5Properties

interface ISession {

    val clientId: String
    var connected: Boolean
    var will: Will?
    var sessionExpiryInterval: UInt
    var sessionDisconnectedTimestamp: Long?
    var mqttVersion: Int

    fun publish(
        retain: Boolean,
        topicName: String,
        qos: Qos,
        dup: Boolean,
        properties: MQTT5Properties?,
        payload: UByteArray?
    )

    fun disconnectClientSessionTakenOver()

    fun checkKeepAliveExpired()

    private fun getExpiryTime(): Long? {
        return if (sessionExpiryInterval == 0xFFFFFFFFu || connected) // If connected it doesn't expire
            null
        else
            sessionDisconnectedTimestamp?.plus((sessionExpiryInterval.toLong() * 1000))
    }

    fun isExpired(): Boolean {
        val timestamp = getExpiryTime()
        val currentTime = currentTimeMillis()
        return timestamp != null && timestamp <= currentTime
    }
}
