package integration

import MQTTClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.broker.Broker
import mqtt.packets.Qos
import mqtt.packets.mqttv5.ReasonCode
import mqtt.packets.mqttv5.SubscriptionOptions
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class RetainedPublishTest {

    @Test
    fun testRetained() = runTest {
        val qos0Topic = "from/qos 0"
        val qos1Topic = "from/qos 1"
        val qos2Topic = "from/qos 2"
        val wildcardTopic = "from/+"

        var received = 0

        val broker = Broker()
        val client = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, null) {
            when (it.topicName) {
                qos0Topic -> {
                    assertContentEquals("qos 0".encodeToByteArray().toUByteArray(), it.payload)
                    assertEquals(Qos.AT_MOST_ONCE, it.qos)
                }
                qos1Topic -> {
                    assertContentEquals("qos 1".encodeToByteArray().toUByteArray(), it.payload)
                    assertEquals(Qos.AT_LEAST_ONCE, it.qos)
                }
                qos2Topic -> {
                    assertContentEquals("qos 2".encodeToByteArray().toUByteArray(), it.payload)
                    assertEquals(Qos.EXACTLY_ONCE, it.qos)
                }
            }
            received++
        }
        broker.step()

        client.publish(true, Qos.AT_MOST_ONCE, qos0Topic, "qos 0".encodeToByteArray().toUByteArray())
        client.publish(true, Qos.AT_LEAST_ONCE, qos1Topic, "qos 1".encodeToByteArray().toUByteArray())
        client.publish(true, Qos.EXACTLY_ONCE, qos2Topic, "qos 2".encodeToByteArray().toUByteArray())

        broker.step()

        client.subscribe(listOf(Subscription(wildcardTopic, SubscriptionOptions(Qos.EXACTLY_ONCE))))

        var i = 0
        while (received < 3 && i < 1000) {
            broker.step()
            client.step()
            i++
            withContext(Dispatchers.Default) {
                delay(10)
            }
        }

        client.disconnect(ReasonCode.SUCCESS)

        broker.stop()

        if (i >= 1000) {
            throw Exception("Test timeout")
        }
    }
}
