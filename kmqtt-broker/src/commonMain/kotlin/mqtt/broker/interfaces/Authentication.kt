package mqtt.broker.interfaces

public interface Authentication {

    /**
     * Checks if the client is allowed to connect to the broker
     * @param clientId the MQTT clientId assigned to the client
     * @param username the MQTT username provided in the CONNECT packet, if present, null otherwise
     * @param password the MQTT password provided in the CONNECT packet, if present, null otherwise
     */
    public fun authenticate(clientId: String, username: String?, password: UByteArray?): Boolean
}
