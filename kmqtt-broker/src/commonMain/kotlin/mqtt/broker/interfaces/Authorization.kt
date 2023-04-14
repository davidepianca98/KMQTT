package mqtt.broker.interfaces

public interface Authorization {

    /**
     * Checks if the client is allowed to do the specified operation
     * @param clientId the MQTT clientId assigned to the client
     * @param username the MQTT username provided in the CONNECT packet, if present, null otherwise
     * @param password the MQTT password provided in the CONNECT packet, if present and broker parameter savePassword=true, null otherwise
     * @param topicName the topic of the PUBLISH or SUBSCRIBE packet
     * @param isSubscription true if the packet received is a SUBSCRIBE, false if it's a PUBLISH
     * @param payload the content of the PUBLISH message, present only if isSubscription is false
     * @return true if the client is allowed to publish or subscribe, false otherwise
     */
    public fun authorize(
        clientId: String,
        username: String?,
        password: UByteArray?,
        topicName: String,
        isSubscription: Boolean,
        payload: UByteArray?
    ): Boolean
}
