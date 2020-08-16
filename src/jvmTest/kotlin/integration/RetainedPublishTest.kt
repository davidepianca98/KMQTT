package integration

import com.hivemq.client.mqtt.datatypes.MqttQos
import mqtt.broker.Broker
import org.junit.Test
import kotlin.test.assertEquals

class RetainedPublishTest {

    @Test
    fun testRetained() {
        val qos0Topic = "from/qos 0"
        val qos1Topic = "from/qos 1"
        val qos2Topic = "from/qos 2"
        val wildcardTopic = "from/+"

        brokerTest(Broker()) { broker ->
            val client = broker.buildClient("client1")
            client.connect()

            client.publishWith().topic(qos0Topic).qos(MqttQos.AT_MOST_ONCE).payload("qos 0".toByteArray()).retain(true)
                .send()
            client.publishWith().topic(qos1Topic).qos(MqttQos.AT_LEAST_ONCE).payload("qos 1".toByteArray()).retain(true)
                .send()
            client.publishWith().topic(qos2Topic).qos(MqttQos.EXACTLY_ONCE).payload("qos 2".toByteArray()).retain(true)
                .send()

            client.disconnect()

            Thread.sleep(1000)

            client.connect()

            client.subscribeAndReceive(wildcardTopic, count = 3) {
                assertEquals("qos 0", String(it[0].payloadAsBytes))
                assertEquals(MqttQos.AT_MOST_ONCE, it[0].qos)
                assertEquals(qos0Topic, it[0].topic.toString())
                assertEquals("qos 1", String(it[1].payloadAsBytes))
                assertEquals(MqttQos.AT_MOST_ONCE, it[1].qos)
                assertEquals(qos1Topic, it[1].topic.toString())
                assertEquals("qos 2", String(it[2].payloadAsBytes))
                assertEquals(MqttQos.AT_MOST_ONCE, it[2].qos)
                assertEquals(qos2Topic, it[2].topic.toString())
            }
        }
    }
}
