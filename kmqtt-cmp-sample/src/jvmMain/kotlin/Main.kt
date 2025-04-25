import io.github.davidepianca98.MQTTClient
import io.github.davidepianca98.mqtt.Subscription
import io.github.davidepianca98.mqtt.packets.Qos
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
public fun main() {
    val scope = CoroutineScope(Dispatchers.Default)
    println("Hello Ktor Client")
    val client = MQTTClient(
        address = "127.0.0.1", port = 1884,
        publishReceived = {
            println(it.payload?.toByteArray()?.let { bytes -> "Publish received ${bytes.decodeToString()}" })
        },
        onSubscribed = {
            println("Subscribed to $it")
        },
    )

    val subscription = Subscription("test-incoming")
    client.subscribe(listOf(subscription))

    scope.launch() {
        while (true) {
            delay(1000)

            println("Publishing...")
            client.publish(
                retain = false,
                qos = Qos.EXACTLY_ONCE,
                topic = "test",
                payload = "hello world ${Clock.System.now()}".toByteArray().toUByteArray()
            )
        }
    }

    client.run()
}
