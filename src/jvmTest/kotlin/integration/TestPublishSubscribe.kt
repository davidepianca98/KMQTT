package integration

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import mqtt.Broker
import org.junit.Test
import kotlin.test.assertEquals


class TestPublishSubscribe {

    private fun testPublishSubscribe(broker: Broker, topic: String, qos: MqttQos, payload: ByteArray, subscribeReceived: (publish: Mqtt5Publish) -> Unit) {
        val brokerThread = BrokerThread(broker)
        brokerThread.start()

        var completed = false

        val client = Mqtt5Client.builder()
            .serverHost(broker.host)
            .serverPort(broker.port)
            .buildAsync()

        client.connect().thenCompose {
            client.subscribeWith()
                .topicFilter(topic)
                .qos(qos)
                .callback {
                    subscribeReceived(it)
                    completed = true
                    client.disconnect()
                    brokerThread.stopBroker()
                }
                .send()
        }.thenApply {
            client.publishWith()
                .topic(topic)
                .qos(qos)
                .payload(payload)
                .send()
        }.exceptionally {
            throw it
        }
        brokerThread.join(10000)
        if(!completed)
            throw Exception("Timeout")
    }

    @Test
    fun testPublishQos0() {
        val sendPayload = "Test"

        testPublishSubscribe(Broker(), "test/topic", MqttQos.AT_MOST_ONCE, sendPayload.toByteArray()) {
            val buffer = it.payload.get()
            val payload = ByteArray(buffer.remaining())
            buffer.get(payload)
            assertEquals(sendPayload, String(payload))
        }
    }

    @Test
    fun testPublishQos1() {
        val sendPayload = "Test"

        testPublishSubscribe(Broker(), "test/topic", MqttQos.AT_LEAST_ONCE, sendPayload.toByteArray()) {
            val buffer = it.payload.get()
            val payload = ByteArray(buffer.remaining())
            buffer.get(payload)
            assertEquals(sendPayload, String(payload))
        }
    }

    @Test
    fun testPublishQos2() {
        val sendPayload = "Test"

        testPublishSubscribe(Broker(), "test/topic", MqttQos.EXACTLY_ONCE, sendPayload.toByteArray()) {
            val buffer = it.payload.get()
            val payload = ByteArray(buffer.remaining())
            buffer.get(payload)
            assertEquals(sendPayload, String(payload))
        }
    }
}
