import mqtt.broker.Broker
import mqtt.broker.interfaces.Authorization

fun main() {
    Broker(serverKeepAlive = 60, authorization = object : Authorization {
        override fun authorize(clientId: String, topicName: String, isSubscription: Boolean): Boolean {
            return topicName != "test/nosubscribe"
        }
    }).listen()
}
