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

class PublishSubscribeMultipleClientsTest {

    private suspend fun testPublish(qos: Qos, topic: String, payload: UByteArray) {
        var received = false

        val broker = Broker()
        val client1 = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, null, clientId = "client1") {}
        val client2 = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, null, clientId = "client2") {
            assertEquals(topic, it.topicName)
            assertContentEquals(payload, it.payload)
            assertEquals(qos, it.qos)
            received = true
        }

        broker.step()

        client2.subscribe(listOf(Subscription(topic, SubscriptionOptions(qos))))

        broker.step()

        client1.publish(false, qos, topic, payload)

        var i = 0
        while (!received && i < 1000) {
            broker.step()
            client1.step()
            client2.step()
            i++
            withContext(Dispatchers.Default) {
                delay(10)
            }
        }

        client1.disconnect(ReasonCode.SUCCESS)
        client2.disconnect(ReasonCode.SUCCESS)
        broker.stop()

        if (i >= 1000) {
            throw Exception("Test timeout")
        }
    }

    @Test
    fun testPublishSubscribeTopicQos0() = runTest {
        val sendPayload = "Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1".encodeToByteArray()
        val topic = "test/topic"

        testPublish(Qos.AT_MOST_ONCE, topic, sendPayload.toUByteArray())
    }

    @Test
    fun testPublishSubscribeTopicQos1() = runTest {
        val sendPayload = "Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1".encodeToByteArray()
        val topic = "test/topic"

        testPublish(Qos.AT_LEAST_ONCE, topic, sendPayload.toUByteArray())
    }

    @Test
    fun testPublishSubscribeTopicQos2() = runTest {
        val sendPayload = "Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1".encodeToByteArray()
        val topic = "test/topic"

        testPublish(Qos.EXACTLY_ONCE, topic, sendPayload.toUByteArray())
    }
}
