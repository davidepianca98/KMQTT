package io.github.davidepianca98.mqtt.broker

import io.github.davidepianca98.currentTimeMillis
import io.github.davidepianca98.mqtt.Will
import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Properties

internal interface ISession {

    val clientId: String
    var connected: Boolean
    var will: Will?
    var sessionExpiryInterval: UInt
    var sessionDisconnectedTimestamp: Long?
    var mqttVersion: MQTTVersion

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
