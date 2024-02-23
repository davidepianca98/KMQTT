package integration

import MQTTClient
import com.goncalossilva.resources.Resource
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
import socket.tls.TLSClientSettings
import socket.tls.TLSSettings
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TLSTest {

    private suspend fun runTest(pathPrefix: String) {
        val sendPayload = "Test"
        val topic = "test/topic"

        var received = false

        val broker = Broker(port = 8883, tlsSettings = TLSSettings(keyStoreFilePath = pathPrefix + "keyStore.p12", keyStorePassword = "changeit"))
        val client = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, TLSClientSettings(serverCertificate = pathPrefix + "cert.pem")) {
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

        client.disconnect(ReasonCode.SUCCESS)

        broker.stop()

        if (i >= 1000) {
            throw Exception("Test timeout")
        }
    }

    @Test
    fun testPublish() = runTest {
        if (Resource("src/commonTest/resources/keyStore.p12").exists()) {
            runTest("src/commonTest/resources/")
        } else {
            runTest("kotlin/")
        }
    }
}
