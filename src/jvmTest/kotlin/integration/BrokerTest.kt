package integration

import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import mqtt.broker.Broker
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

fun Broker.buildClient(id: String): Mqtt5BlockingClient {
    return Mqtt5Client.builder()
        .serverHost(host)
        .serverPort(port)
        .identifier(id)
        .buildBlocking()
}

fun brokerTest(broker: Broker, test: (broker: Broker) -> Unit) {
    val brokerThread = BrokerThread(broker)
    try {
        brokerThread.start()
        test(broker)
    } finally {
        brokerThread.stopBroker()
        brokerThread.join()
    }
}

fun Mqtt5BlockingClient.subscribeAndReceive(
    topic: String,
    count: Int = 1,
    callback: (publishMessages: List<Mqtt5Publish>) -> Unit
) {
    val client = this
    client.publishes(MqttGlobalPublishFilter.ALL).use { publishes ->
        client.subscribeWith().topicFilter(topic).qos(MqttQos.AT_MOST_ONCE).send()
        val messages = mutableListOf<Mqtt5Publish>()
        for (i in 0 until count) {
            val publish = publishes.receive(1, TimeUnit.SECONDS).get()
            messages.add(publish)
        }

        assertEquals(count, messages.size)
        callback(messages)
    }
}
