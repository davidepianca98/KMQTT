package integration

import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import mqtt.broker.Broker
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class TestPublishSubscribeMultipleClients {

    @Test
    fun testPublishSubscribeTopicQos0() {
        val sendPayload = "Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1".toByteArray()

        brokerTest(Broker()) { broker ->
            val topic = "test/topic"

            val client1 = broker.buildClient("client1")
            val client2 = broker.buildClient("client2")

            client1.connect()
            client2.connect()

            client2.publishes(MqttGlobalPublishFilter.ALL).use { publishes ->
                client2.subscribeWith().topicFilter(topic).qos(MqttQos.AT_MOST_ONCE).send()

                client1.publishWith().topic(topic).qos(MqttQos.AT_MOST_ONCE).payload(sendPayload).send()
                val publish = publishes.receive(1, TimeUnit.SECONDS).get()
                assertEquals(topic, publish.topic.toString())
                assertArrayEquals(sendPayload, publish.payloadAsBytes)
                assertEquals(MqttQos.AT_MOST_ONCE, publish.qos)
            }

            client1.disconnect()
            client2.disconnect()
        }
    }

    @Test
    fun testPublishSubscribeTopicQos1() {
        val sendPayload = "Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1".toByteArray()

        brokerTest(Broker()) { broker ->
            val topic = "test/topic"

            val client1 = broker.buildClient("client1")
            val client2 = broker.buildClient("client2")

            client1.connect()
            client2.connect()

            client2.publishes(MqttGlobalPublishFilter.ALL).use { publishes ->
                client2.subscribeWith().topicFilter(topic).qos(MqttQos.AT_LEAST_ONCE).send()

                client1.publishWith().topic(topic).qos(MqttQos.AT_LEAST_ONCE).payload(sendPayload).send()
                val publish = publishes.receive(1, TimeUnit.SECONDS).get()
                assertEquals(topic, publish.topic.toString())
                assertArrayEquals(sendPayload, publish.payloadAsBytes)
                assertEquals(MqttQos.AT_LEAST_ONCE, publish.qos)
            }

            client1.disconnect()
            client2.disconnect()
        }
    }

    @Test
    fun testPublishSubscribeTopicQos2() {
        val sendPayload = "Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1Test1".toByteArray()

        brokerTest(Broker()) { broker ->
            val topic = "test/topic"

            val client1 = broker.buildClient("client1")
            val client2 = broker.buildClient("client2")

            client1.connect()
            client2.connect()

            client2.publishes(MqttGlobalPublishFilter.ALL).use { publishes ->
                client2.subscribeWith().topicFilter(topic).qos(MqttQos.AT_LEAST_ONCE).send()

                client1.publishWith().topic(topic).qos(MqttQos.EXACTLY_ONCE).payload(sendPayload).send()
                val publish = publishes.receive(1, TimeUnit.SECONDS).get()
                assertEquals(topic, publish.topic.toString())
                assertArrayEquals(sendPayload, publish.payloadAsBytes)
                assertEquals(MqttQos.AT_LEAST_ONCE, publish.qos)
            }

            client1.disconnect()
            client2.disconnect()
        }
    }
}
