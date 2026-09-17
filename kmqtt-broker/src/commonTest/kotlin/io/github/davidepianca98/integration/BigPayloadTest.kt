package io.github.davidepianca98.integration

import io.github.davidepianca98.MQTTClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.Subscription
import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Properties
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.mqtt.packets.mqttv5.SubscriptionOptions
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class BigPayloadTest {

    // 1 MiB payload vs 16 KiB socket buffers: the packet must be reassembled
    // from many socket reads on both the broker and the subscribing client
    private val sendPayload = UByteArray(1024 * 1024) { (it % 256).toUByte() }
    private val topic = "test/big"

    private suspend fun runBigPayloadTest(port: Int, qos: Qos) {
        var received = false

        val broker = Broker(port = port, maximumPacketSize = 4194304u)
        val clientProperties = MQTT5Properties(maximumPacketSize = 4194304u)
        val subscriber = MQTTClient(
            MQTTVersion.MQTT5, "127.0.0.1", broker.port, null,
            properties = clientProperties,
        ) {
            assertEquals(topic, it.topicName)
            assertContentEquals(sendPayload, it.payload)
            assertEquals(qos, it.qos)
            received = true
        }
        broker.step()
        val publisher = MQTTClient(
            MQTTVersion.MQTT5, "127.0.0.1", broker.port, null,
            properties = clientProperties,
        ) {}
        broker.step()

        subscriber.subscribe(listOf(Subscription(topic, SubscriptionOptions(qos))))
        broker.step()
        subscriber.step()

        publisher.publish(false, qos, topic, sendPayload)

        var i = 0
        while (!received && i < 1000) {
            broker.step()
            publisher.step()
            subscriber.step()
            i++
            delay(10)
        }

        publisher.disconnect(ReasonCode.SUCCESS)
        subscriber.disconnect(ReasonCode.SUCCESS)
        broker.stop()

        if (i >= 1000) {
            throw Exception("Test timeout")
        }
    }

    @Test
    fun testBigPayloadQos0() = runTest {
        runBigPayloadTest(2886, Qos.AT_MOST_ONCE)
    }

    @Test
    fun testBigPayloadQos1() = runTest {
        runBigPayloadTest(2887, Qos.AT_LEAST_ONCE)
    }
}
