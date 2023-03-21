package mqtt.packets.mqttv5

import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.MQTT5Subscribe
import mqtt.packets.mqttv5.SubscriptionOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MQTTSubscribeTest {

    private val array = ubyteArrayOf(
        0x82u,
        0x12u,
        0x00u,
        0x41u,
        0x00u,
        0x00u,
        0x0Cu,
        0x74u,
        0x65u,
        0x73u,
        0x74u,
        0x2Fu,
        0x74u,
        0x6Fu,
        0x70u,
        0x69u,
        0x63u,
        0x2Fu,
        0x2Bu,
        0x00u
    )
    private val packet = MQTT5Subscribe(
        65u,
        listOf(
            Subscription(
                "test/topic/+",
                SubscriptionOptions(Qos.AT_MOST_ONCE, false, false, 0u)
            )
        )
    )

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTT5Subscribe.fromByteArray(2, array.copyOfRange(2, array.size))
        assertEquals(packet.packetIdentifier, result.packetIdentifier)
        assertEquals(packet.subscriptions[0].topicFilter, result.subscriptions[0].topicFilter)
        assertEquals(packet.subscriptions[0].options.qos, result.subscriptions[0].options.qos)
    }
}
