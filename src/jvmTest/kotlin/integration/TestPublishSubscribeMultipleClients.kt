package integration

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import mqtt.broker.Broker
import org.junit.Test
import kotlin.test.assertEquals

class TestPublishSubscribeMultipleClients {

    private fun buildClient(id: String, broker: Broker): Mqtt5AsyncClient {
        return Mqtt5Client.builder()
            .serverHost(broker.host)
            .serverPort(broker.port)
            .identifier(id)
            .buildAsync()
    }

    private fun testPublishSubscribeResponseTopic(
        broker: Broker,
        topic: String,
        responseTopic: String,
        qos: MqttQos,
        payload: ByteArray,
        subscribeReceived: (publish: Mqtt5Publish) -> Unit
    ) {
        val brokerThread = BrokerThread(broker)
        brokerThread.start()

        var completed = false

        val client1 = buildClient("client1", broker)
        val client2 = buildClient("client2", broker)

        client2.connect().thenCompose {
            client2.subscribeWith()
                .topicFilter(topic)
                .qos(qos)
                .callback {
                    client2.publishWith()
                        .topic(it.responseTopic.get())
                        .qos(qos)
                        .payload(it.payloadAsBytes)
                        .send()
                }
                .send()
        }

        client1.connect().thenCompose {
            client1.subscribeWith()
                .topicFilter(responseTopic)
                .qos(qos)
                .callback {
                    subscribeReceived(it)
                    completed = true
                    client1.disconnect()
                    client2.disconnect()
                    brokerThread.stopBroker()
                }
                .send()
        }.thenApply {
            client1.publishWith()
                .topic(topic)
                .qos(qos)
                .responseTopic(responseTopic)
                .payload(payload)
                .send()
        }.exceptionally {
            throw it
        }
        brokerThread.join(10000)
        if (!completed)
            throw Exception("Timeout")
    }

    @Test
    fun testPublishResponseTopicQos0() {
        val sendPayload = "Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1"

        testPublishSubscribeResponseTopic(
            Broker(),
            "test/topic",
            "test/topic/response",
            MqttQos.AT_MOST_ONCE,
            sendPayload.toByteArray()
        ) {
            val payload = it.payloadAsBytes
            assertEquals(sendPayload, String(payload))
        }
    }
}
