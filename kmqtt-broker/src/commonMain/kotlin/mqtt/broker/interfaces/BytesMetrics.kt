package mqtt.broker.interfaces

interface BytesMetrics {

    /**
     * Called when a client receives a packet
     * @param clientId the clientId of the client that received the packet
     * @param bytesCount the size of the received packet
     */
    fun received(clientId: String, bytesCount: Long)

    /**
     * Called when a client sends a packet
     * @param clientId the clientId of the client that sent the packet
     * @param bytesCount the size of the sent packet
     */
    fun sent(clientId: String, bytesCount: Long)
}
