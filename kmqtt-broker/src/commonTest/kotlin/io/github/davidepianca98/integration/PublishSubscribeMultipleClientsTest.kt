package io.github.davidepianca98.integration

import io.github.davidepianca98.MQTTClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.Subscription
import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.mqtt.packets.mqttv5.SubscriptionOptions
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PublishSubscribeMultipleClientsTest {

    private suspend fun testPublish(qos: Qos, topic: String, payload: UByteArray) {
        var received = false

        val broker = Broker()

        val clientPub = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, null, clientId = "client1") {}

        broker.step()
        clientPub.step()

        val clientSub = MQTTClient(
            MQTTVersion.MQTT5,
            "127.0.0.1",
            broker.port,
            null,
            clientId = "client2",
            onSubscribed = {
                clientPub.publish(false, qos, topic, payload)
            }
        ) {
            assertEquals(topic, it.topicName)
            assertContentEquals(payload, it.payload)
            assertEquals(qos, it.qos)
            received = true
        }
        broker.step()

        clientSub.subscribe(listOf(Subscription(topic, SubscriptionOptions(qos))))

        var i = 0
        while (!received && i < 1000) {
            broker.step()
            clientPub.step()
            clientSub.step()
            i++
            delay(10)
        }

        clientPub.disconnect(ReasonCode.SUCCESS)
        clientSub.disconnect(ReasonCode.SUCCESS)
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
