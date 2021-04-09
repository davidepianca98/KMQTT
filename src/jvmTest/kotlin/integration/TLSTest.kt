package integration

import mqtt.broker.Broker
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import socket.tls.TLSSettings

class TLSTest {

    private fun testPublishSubscribe(
        broker: Broker,
        topic: String,
        payload: ByteArray
    ) {
        val client = broker.buildTLSClient("client1")

        val options = MqttConnectionOptions()
        options.socketFactory = sslSocketFactory()
        client.connect(options)

        client.setCallback(object : MqttCallback {
            override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
                throw Exception("test failed")
            }

            override fun mqttErrorOccurred(exception: MqttException?) {
                throw Exception("test failed")
            }

            override fun messageArrived(topicRec: String?, message: MqttMessage?) {
                assertEquals(topic, topicRec)
                Assert.assertArrayEquals(payload, message?.payload)
                assertEquals(2, message?.qos)
            }

            override fun deliveryComplete(token: IMqttToken?) {
                throw Exception("test failed")
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                throw Exception("test failed")
            }

            override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
                throw Exception("test failed")
            }
        })
        client.subscribe(topic, 2)
        client.publish(topic, payload, 1, false)

        client.disconnect()
    }

    @Test
    fun testPublish() {

        val sendPayload = "Test"

        brokerTest(
            Broker(
                host = "localhost",
                port = 8883,
                tlsSettings = TLSSettings(keyStoreFilePath = "docker/linux/keyStore.p12", keyStorePassword = "changeit")
            )
        ) { broker ->
            testPublishSubscribe(broker, "test/topic", sendPayload.toByteArray())
        }
    }
}
