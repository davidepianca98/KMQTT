package integration

import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import mqtt.broker.Broker
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals


class PublishSubscribeSingleClientTest {

    private fun testPublishSubscribe(
        broker: Broker,
        topic: String,
        payload: ByteArray
    ) {
        val client = broker.buildClient("client1")

        client.connect()
        client.publishes(MqttGlobalPublishFilter.ALL).use { publishes ->
            client.subscribeWith().topicFilter(topic).qos(MqttQos.AT_MOST_ONCE).send()

            client.publishWith().topic(topic).qos(MqttQos.AT_LEAST_ONCE).payload(payload).send()
            val publish = publishes.receive(1, TimeUnit.SECONDS).get()
            assertEquals(topic, publish.topic.toString())
            Assert.assertArrayEquals(payload, publish.payloadAsBytes)
            assertEquals(MqttQos.AT_MOST_ONCE, publish.qos)
        }

        client.disconnect()
    }

    @Test
    fun testPublish() {
        val sendPayload = "Test"

        brokerTest(Broker()) { broker ->
            testPublishSubscribe(broker, "test/topic", sendPayload.toByteArray())
        }
    }
}
