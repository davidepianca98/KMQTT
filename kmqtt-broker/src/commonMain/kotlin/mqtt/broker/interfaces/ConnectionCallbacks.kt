package mqtt.broker.interfaces

interface ConnectionCallbacks {

    /**
     * Called when the client disconnects from the broker or times out
     * @param clientId the MQTT clientId assigned to the client
     * @param timeout true if MQTT keep-alive expired, false otherwise
     */
    fun onDisconnect(clientId: String, timeout: Boolean)
}
