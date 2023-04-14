package mqtt.broker.interfaces

public interface BytesMetrics {

    /**
     * Called when a client receives a packet
     * @param clientId the clientId of the client that received the packet
     * @param bytesCount the size of the received packet
     */
    public fun received(clientId: String, bytesCount: Long)

    /**
     * Called when a client sends a packet
     * @param clientId the clientId of the client that sent the packet
     * @param bytesCount the size of the sent packet
     */
    public fun sent(clientId: String, bytesCount: Long)
}
