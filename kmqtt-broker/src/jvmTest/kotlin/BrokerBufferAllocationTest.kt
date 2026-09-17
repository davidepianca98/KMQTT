import io.github.davidepianca98.MQTTClient
import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class BrokerBufferAllocationTest {

    @Test
    fun testAcceptWithProtocolMaximumPacketSizeDoesNotPreallocateBuffers() = runTest {
        // With eager allocation the broker would allocate 2 x 256 MB on accept
        // and OOM under the pinned 256m test heap
        val broker = Broker(port = 2888, maximumPacketSize = 268435455u)
        val client = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, null) {}

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
