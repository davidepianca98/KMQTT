package mqtt.broker

import mqtt.packets.MQTTPacket
import mqtt.packets.mqttv5.ReasonCode
import socket.SocketInterface
import socket.tcp.TCPEventHandler

class ClientConnection4(
    private val client: SocketInterface,
    private val broker: Broker
) : TCPEventHandler, ClientConnection(client, broker) {
    override fun disconnect(reasonCode: ReasonCode, serverReference: String?) {
        TODO("Not yet implemented")
    }

    override fun handleConnect(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }

    override fun handlePublish(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }

    override fun handlePuback(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }

    override fun handlePubrec(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }

    override fun handlePubrel(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }

    override fun handlePubcomp(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }

    override fun handleSubscribe(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }

    override fun handleUnsubscribe(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }

    override fun handlePingreq(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }

    override fun handleDisconnect(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }

    override fun handleAuth(packet: MQTTPacket) {
        TODO("Not yet implemented")
    }
}
