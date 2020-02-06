package mqtt.broker

interface EnhancedAuthenticationProvider {

    enum class Result {
        SUCCESS,
        ERROR,
        NEEDS_MORE
    }

    /**
     * Gets called upon reception of a CONNECT packet with the Authentication Method property set or upon reception
     * of an AUTH packet
     * @param clientId the requested or assigned Client ID if none is given by the client
     * @param authenticationData the Authentication Data received if present in the received packet
     * @param result function to call to continue the authentication or to set it as complete
     */
    fun authReceived(
        clientId: String,
        authenticationData: UByteArray?,
        result: (completed: Result, authenticationData: UByteArray?) -> Unit
    )
}
