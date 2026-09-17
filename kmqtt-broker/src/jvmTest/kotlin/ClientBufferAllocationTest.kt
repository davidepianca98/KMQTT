import io.github.davidepianca98.MQTTClient
import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Properties
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ClientBufferAllocationTest {

    @Test
    fun testConnectWithProtocolMaximumPacketSizeDoesNotPreallocateBuffers() = runTest {
        // 268435455 = MQTT protocol maximum; with eager allocation this would
        // require 2 x 256 MB on the client alone and OOM under the pinned 256m heap
        // Discriminating power comes from maxHeapSize = "256m" pinned on this module's jvm test task
        val broker = Broker(port = 2885)
        val client = MQTTClient(
            MQTTVersion.MQTT5,
            "127.0.0.1",
            broker.port,
            null,
            properties = MQTT5Properties(maximumPacketSize = 268435455u),
        ) {}

        var i = 0
        while (!client.isConnackReceived() && i < 1000) {
            broker.step()
            client.step()
            i++
            delay(10)
        }

        client.disconnect(ReasonCode.SUCCESS)
        broker.stop()

        if (i >= 1000) {
            throw Exception("Test timeout")
        }
    }
}
