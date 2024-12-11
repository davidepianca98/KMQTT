package io.github.davidepianca98.mqtt.broker.interfaces

import io.github.davidepianca98.mqtt.packets.MQTTPacket

public interface PacketInterceptor {

    /**
     * Called when a packet is received from a client
     * @param clientId the clientId assigned to the MQTT client that sent the packet
     * @param username the MQTT username provided in the CONNECT packet, if present, null otherwise
     * @param password the MQTT password provided in the CONNECT packet, if present and broker parameter savePassword=true, null otherwise
     * @param packet the MQTT packet sent by the client
     */
    public fun packetReceived(clientId: String, username: String?, password: UByteArray?, packet: MQTTPacket)
}
