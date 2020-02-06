package mqtt.broker

interface Authorization {

    fun authorize(topicName: String): Boolean
}
