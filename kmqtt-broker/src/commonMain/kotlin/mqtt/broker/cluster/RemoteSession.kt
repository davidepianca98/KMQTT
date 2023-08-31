package mqtt.broker.cluster

import mqtt.MQTTVersion
import mqtt.Will
import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import mqtt.broker.ISession
import mqtt.broker.Session
import mqtt.packets.Qos
import mqtt.packets.mqttv4.MQTT4Publish
import mqtt.packets.mqttv5.MQTT5Properties
import mqtt.packets.mqttv5.MQTT5Publish

internal class RemoteSession(
    private val connection: ClusterConnection,
    override val clientId: String,
    override var sessionExpiryInterval: UInt,
    override var sessionDisconnectedTimestamp: Long?,
    override var will: Will? = null
) : ISession {

    override var connected = false
    override var mqttVersion = MQTTVersion.MQTT3_1_1

    // TODO if client connects and a remote session is found for that clientid, request the full session
    //      if persistence is enabled then at startup all the brokers will think the sessions belong to them,
    //      as soon as a client connects, let the other brokers know that so they change it to remote session

    override fun publish(
        retain: Boolean,
        topicName: String,
        qos: Qos,
        dup: Boolean,
        properties: MQTT5Properties?,
        payload: UByteArray?
    ) {
        val packet = if (mqttVersion == MQTTVersion.MQTT5) {
            MQTT5Publish(
                retain,
                qos,
                dup,
                topicName,
                0u,
                properties!!,
                payload
            )
        } else {
            MQTT4Publish(
                retain,
                qos,
                dup,
                topicName,
                0u,
                payload
            )
        }
        connection.publish(packet)
    }

    override fun disconnectClientSessionTakenOver() {
        connection.sessionTakenOver(clientId)
    }

    override fun checkKeepAliveExpired() {

    }

    fun toLocalSession(clientConnection: ClientConnection, broker: Broker): Session {
        return Session(
            clientConnection,
            clientId,
            0u, // TODO sessionExpiryInterval
            null, // TODO will
            clientConnection::persistSession,
            broker::propagateSession
        )
    }

}
