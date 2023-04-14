package mqtt.broker.cluster

import mqtt.Subscription
import mqtt.broker.Broker
import mqtt.broker.Session
import mqtt.broker.cluster.packets.*
import socket.SocketInterface
import socket.tcp.TCPEventHandler

internal class ClusterConnection(private val socket: SocketInterface, private val broker: Broker) : TCPEventHandler {

    private val currentReceivedPacket = ClusterCurrentPacket()

    override fun read(): UByteArray? {
        return socket.read()
    }

    override fun dataReceived(data: UByteArray) {
        currentReceivedPacket.addData(data).forEach {
            handlePacket(it)
        }
    }

    private fun handlePacket(packet: ClusterPacket) {
        when (packet) {
            is SetRetainedPacket -> {
                broker.setRetained(packet.retained.first.topicName, packet.retained.first, packet.retained.second, true)
            }
            is AddSubscriptionPacket -> {
                broker.addSubscription(packet.clientId, packet.subscription, true)
            }
            is RemoveSubscriptionPacket -> {
                broker.removeSubscription(packet.clientId, packet.topicFilter)
            }
            is PublishPacket -> {
                broker.publishFromRemote(packet.packet)
            }
            is AddSessionPacket -> {
                broker.addSession(
                    packet.clientId,
                    RemoteSession(this, packet.clientId, packet.sessionExpiryInterval, null)
                )
            }
            is SessionUpdatePacket -> {
                val session = RemoteSession(
                    this,
                    packet.clientId,
                    packet.sessionExpiryInterval,
                    packet.sessionDisconnectedTimestamp
                )
                if (packet.connected)
                    session.connected = packet.connected
                broker.addSession(packet.clientId, session)
            }
            is SessionTakenOverPacket -> {
                broker.getSession(packet.clientId)?.disconnectClientSessionTakenOver()
            }
        }
    }

    override fun sendRemaining() {
        socket.sendRemaining()
    }

    override fun closedGracefully() {
        socket.close()
    }

    override fun closedWithException() {
        socket.close()
    }

    fun publish(packet: mqtt.packets.mqtt.MQTTPublish) {
        socket.send(PublishPacket(packet).toByteArray())
    }

    fun setRetained(retained: Pair<mqtt.packets.mqtt.MQTTPublish, String>) {
        socket.send(SetRetainedPacket(retained).toByteArray())
    }

    fun addSubscription(clientId: String, subscription: Subscription) {
        socket.send(AddSubscriptionPacket(clientId, subscription).toByteArray())
    }

    fun removeSubscription(clientId: String, topicFilter: String) {
        socket.send(RemoveSubscriptionPacket(clientId, topicFilter).toByteArray())
    }

    fun addSession(session: Session) {
        socket.send(AddSessionPacket(session.clientId, session.sessionExpiryInterval).toByteArray())
    }

    fun updateSession(session: Session) {
        socket.send(
            SessionUpdatePacket(
                session.clientId,
                session.connected,
                session.sessionDisconnectedTimestamp,
                session.sessionExpiryInterval
            ).toByteArray()
        )
    }

    fun sessionTakenOver(clientId: String) {
        socket.send(SessionTakenOverPacket(clientId).toByteArray())
    }
}
