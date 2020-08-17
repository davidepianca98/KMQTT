package mqtt.broker

import currentTimeMillis
import generateRandomClientId
import mqtt.MQTTCurrentPacket
import mqtt.MQTTException
import mqtt.packets.MQTTPacket
import mqtt.packets.mqttv5.*
import socket.SocketInterface
import socket.streams.EOFException
import socket.tcp.IOException
import socket.tcp.TCPEventHandler

abstract class ClientConnection(private val client: SocketInterface, private val broker: Broker) : TCPEventHandler {

    internal var clientId: String? = null
    internal var session: Session? = null

    private val currentReceivedPacket = MQTTCurrentPacket(broker.maximumPacketSize)
    private var lastReceivedMessageTimestamp = currentTimeMillis()

    internal var keepAlive = 0
    private var connectHandled = false

    abstract fun disconnect(reasonCode: ReasonCode, serverReference: String? = null)

    abstract fun handleConnect(packet: MQTTPacket)

    abstract fun handlePublish(packet: MQTTPacket)

    abstract fun handlePuback(packet: MQTTPacket)

    abstract fun handlePubrec(packet: MQTTPacket)

    abstract fun handlePubrel(packet: MQTTPacket)

    abstract fun handlePubcomp(packet: MQTTPacket)

    abstract fun handleSubscribe(packet: MQTTPacket)

    abstract fun handleUnsubscribe(packet: MQTTPacket)

    abstract fun handlePingreq(packet: MQTTPacket)

    abstract fun handleDisconnect(packet: MQTTPacket)

    abstract fun handleAuth(packet: MQTTPacket)

    internal fun close() {
        client.close()
        broker.sessions[clientId]?.disconnected()
    }

    override fun closedWithException() {
        close()
    }

    override fun closedGracefully() {
        close()
    }

    override fun read(): UByteArray? {
        return client.read()
    }

    override fun sendRemaining() {
        client.sendRemaining()
    }

    override fun dataReceived(data: UByteArray) {
        lastReceivedMessageTimestamp = currentTimeMillis()
        try {
            clientId?.let {
                broker.bytesMetrics?.received(it, data.size.toLong())
            }
            currentReceivedPacket.addData(data).forEach {
                handlePacket(it)
            }
        } catch (e: MQTTException) {
            disconnect(e.reasonCode)
        } catch (e: EOFException) {
            println("EOF")
            close()
        } catch (e: IOException) {
            println("IOException ${e.message}")
            closedWithException()
        } catch (e: Exception) {
            println("Exception ${e.message} ${e.cause?.message}")
            disconnect(ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR)
        }
    }

    private fun handlePacket(packet: MQTTPacket) {
        if (packet is MQTT5Connect) {
            if (!connectHandled) {
                handleConnect(packet)
                connectHandled = true
            }
        } else {
            if (!connectHandled) // If first packet is not CONNECT, send Protocol Error
                throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            when (packet) {
                is MQTT5Publish -> handlePublish(packet)
                is MQTT5Puback -> handlePuback(packet)
                is MQTT5Pubrec -> handlePubrec(packet)
                is MQTT5Pubrel -> handlePubrel(packet)
                is MQTT5Pubcomp -> handlePubcomp(packet)
                is MQTT5Subscribe -> handleSubscribe(packet)
                is MQTT5Unsubscribe -> handleUnsubscribe(packet)
                is MQTT5Pingreq -> handlePingreq(packet)
                is MQTT5Disconnect -> handleDisconnect(packet)
                is MQTT5Auth -> handleAuth(packet)
                else -> throw MQTTException(ReasonCode.PROTOCOL_ERROR)
            }
        }
        broker.packetInterceptor?.packetReceived(packet)
    }

    fun checkKeepAliveExpired() {
        val timeout = ((keepAlive * 1000).toDouble() * 1.5).toInt()
        val expired = currentTimeMillis() > lastReceivedMessageTimestamp + timeout
        if (expired) {
            if (connectHandled) {
                if (keepAlive > 0) {
                    disconnect(ReasonCode.KEEP_ALIVE_TIMEOUT)
                    session?.disconnected()
                }
            } else {
                disconnect(ReasonCode.MAXIMUM_CONNECT_TIME)
            }
        }
    }

    internal fun generateClientId(): String {
        var id: String
        do {
            id = generateRandomClientId()
        } while (broker.sessions[id] != null)
        return id
    }

    internal fun handleAuthentication(packet: MQTT5Connect) {
        if (broker.authentication != null) {
            if (packet.userName != null || packet.password != null) {
                if (!broker.authentication.authenticate(packet.userName, packet.password)) {
                    throw MQTTException(ReasonCode.NOT_AUTHORIZED)
                }
            } else {
                throw MQTTException(ReasonCode.NOT_AUTHORIZED)
            }
        }
    }
}
