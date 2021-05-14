package mqtt.broker.interfaces

import mqtt.packets.MQTTPacket

interface PacketInterceptor {

    /**
     * Called when a packet is received from a client
     * @param clientId the clientId assigned to the MQTT client that sent the packet
     * @param username the MQTT username provided in the CONNECT packet, if present, null otherwise
     * @param packet the MQTT packet sent by the client
     */
    fun packetReceived(clientId: String, username: String?, packet: MQTTPacket)
}
