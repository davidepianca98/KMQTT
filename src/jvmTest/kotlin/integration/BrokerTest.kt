package integration

import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import mqtt.broker.Broker
import org.eclipse.paho.mqttv5.client.MqttClient
import org.junit.Assert.assertEquals
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager


fun Broker.buildClient(id: String): Mqtt5BlockingClient {
    return Mqtt5Client.builder()
        .serverHost(host)
        .serverPort(port)
        .identifier(id)
        .buildBlocking()
}

class BlindTrustManager : X509TrustManager {

    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
        println(authType)
        println(chain!![0]?.issuerX500Principal)
    }

    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }
}

fun sslSocketFactory(): SSLSocketFactory {
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, arrayOf<X509TrustManager>(BlindTrustManager()), SecureRandom())
    return sslContext.socketFactory
}

fun Broker.buildTLSClient(id: String): MqttClient {
    return MqttClient("ssl://$host:$port", id)
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
