package integration

import MQTTClient
import TLSClientSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.broker.Broker
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import socket.tls.TLSSettings
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TLSTest {

    @Test
    fun testPublish() = runTest {
        val sendPayload = "Test"
        val topic = "test/topic"

        var received = false

        val broker = Broker(port = 8883, tlsSettings = TLSSettings(keyStoreFilePath = "docker/linux/keyStore.p12", keyStorePassword = "changeit"))
        val client = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, TLSClientSettings(serverCertificatePath = "docker/linux/cert.pem")) {
            assertEquals(topic, it.topicName)
            assertContentEquals(sendPayload.encodeToByteArray().toUByteArray(), it.payload)
            assertEquals(Qos.AT_MOST_ONCE, it.qos)
            received = true
        }
        broker.step()

        client.subscribe(listOf(Subscription(topic, SubscriptionOptions(Qos.AT_MOST_ONCE))))

        broker.step()

        client.publish(false, Qos.AT_MOST_ONCE, topic, sendPayload.encodeToByteArray().toUByteArray())

        var i = 0
        while (!received && i < 1000) {
            broker.step()
            client.step()
            i++
            withContext(Dispatchers.Default) {
                delay(10)
            }
        }

        broker.stop()

        if (i >= 1000) {
            throw Exception("Test timeout")
        }
    }
}
