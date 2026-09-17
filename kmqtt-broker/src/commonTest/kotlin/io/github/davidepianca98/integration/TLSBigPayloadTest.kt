package io.github.davidepianca98.integration

import io.github.davidepianca98.MQTTClient
import com.goncalossilva.resources.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.Subscription
import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Properties
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.mqtt.packets.mqttv5.SubscriptionOptions
import io.github.davidepianca98.socket.tls.TLSClientSettings
import io.github.davidepianca98.socket.tls.TLSSettings
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TLSBigPayloadTest {

    // TLS records are ~16.7 KB, larger than the 16 KiB initial socket buffers:
    // the handshake exercises the TLSSocket buffer growth path for real
    private val sendPayload = UByteArray(1024 * 1024) { (it % 256).toUByte() }
    private val topic = "test/big/tls"

    private suspend fun runTest(pathPrefix: String) {
        var received = false

        val broker = Broker(
            port = 8884,
            maximumPacketSize = 4194304u,
            tlsSettings = TLSSettings(keyStoreFilePath = pathPrefix + "keyStore.p12", keyStorePassword = "changeit"),
        )
        val client = MQTTClient(
            MQTTVersion.MQTT5, "127.0.0.1", broker.port,
            TLSClientSettings(serverCertificate = pathPrefix + "cert.pem"),
            properties = MQTT5Properties(maximumPacketSize = 4194304u),
        ) {
            assertEquals(topic, it.topicName)
            assertContentEquals(sendPayload, it.payload)
            assertEquals(Qos.AT_LEAST_ONCE, it.qos)
            received = true
        }
        for (i in 1..5) {
            broker.step()
            client.step()
        }

        client.subscribe(listOf(Subscription(topic, SubscriptionOptions(Qos.AT_LEAST_ONCE))))
        broker.step()
        client.step()

        client.publish(false, Qos.AT_LEAST_ONCE, topic, sendPayload)

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

    @Test
    fun testBigPayloadOverTls() = runTest {
        if (Resource("src/commonTest/resources/keyStore.p12").exists()) {
            runTest("src/commonTest/resources/")
        } else {
            runTest("kotlin/")
        }
    }
}
