import mqtt.broker.Authorization
import mqtt.broker.Broker
import java.nio.ByteBuffer

actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

fun ByteBuffer.toUByteArray(): UByteArray {
    val length = remaining()
    val array = ByteArray(length)
    get(array, 0, length)
    return array.toUByteArray()
}

fun main() {
    Broker(
        serverKeepAlive = 60,
        authorization = object : Authorization {
            override fun authorize(clientId: String, topicName: String, isSubscription: Boolean): Boolean {
                return !(isSubscription && topicName == "test/nosubscribe")
            }
        }
    ).listen()
}
