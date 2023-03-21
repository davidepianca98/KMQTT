package integration

import IgnoreJs
import MQTTClient
import mqtt.Subscription
import mqtt.broker.Broker
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@IgnoreJs
class PublishSubscribeMultipleClientsTest {

    private fun testPublish(qos: Qos, topic: String, payload: UByteArray) {
        var received = false

        val broker = Broker()
        val client1 = MQTTClient(5, "127.0.0.1", broker.port, null, clientId = "client1") {}
        val client2 = MQTTClient(5, "127.0.0.1", broker.port, null, clientId = "client2") {
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
        }

        broker.stop()

        if (i >= 1000) {
            throw Exception("Test timeout")
        }
    }

    @Test
    fun testPublishSubscribeTopicQos0() {
        val sendPayload = "Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1".encodeToByteArray()
        val topic = "test/topic"

        testPublish(Qos.AT_MOST_ONCE, topic, sendPayload.toUByteArray())
    }

    @Test
    fun testPublishSubscribeTopicQos1() {
        val sendPayload = "Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1".encodeToByteArray()
        val topic = "test/topic"

        testPublish(Qos.AT_LEAST_ONCE, topic, sendPayload.toUByteArray())
    }

    @Test
    fun testPublishSubscribeTopicQos2() {
        val sendPayload = "Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1".encodeToByteArray()
        val topic = "test/topic"

        testPublish(Qos.EXACTLY_ONCE, topic, sendPayload.toUByteArray())
    }
}
