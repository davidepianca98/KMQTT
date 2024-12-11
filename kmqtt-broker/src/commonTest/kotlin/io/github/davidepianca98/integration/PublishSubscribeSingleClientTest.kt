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


class PublishSubscribeSingleClientTest {

    @Test
    fun testPublish() = runTest {
        val sendPayload = "Test"
        val topic = "test/topic"

        var received = false

        val broker = Broker()
        val client = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, null) {
            assertEquals(topic, it.topicName)
            assertContentEquals(sendPayload.encodeToByteArray().toUByteArray(), it.payload)
            assertEquals(Qos.AT_MOST_ONCE, it.qos)
            received = true
        }
        broker.step()

        client.subscribe(listOf(Subscription(topic, SubscriptionOptions(Qos.AT_MOST_ONCE))))

        broker.step()

        client.publish(false, Qos.AT_LEAST_ONCE, topic, sendPayload.encodeToByteArray().toUByteArray())

        var i = 0
        while (!received && i < 1000) {
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
