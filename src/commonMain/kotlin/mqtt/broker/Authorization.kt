package mqtt.broker

interface Authorization {

    fun authorize(clientId: String, topicName: String, isSubscription: Boolean): Boolean
}
