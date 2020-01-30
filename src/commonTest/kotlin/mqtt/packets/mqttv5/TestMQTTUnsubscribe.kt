package mqtt.packets.mqttv5

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMQTTUnsubscribe {

    private val array = ubyteArrayOf(
        0xA2u,
        0x1Du,
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
        0x00u,
        0x0Au,
        0x2Fu,
        0x74u,
        0x65u,
        0x73u,
        0x74u,
        0x74u,
        0x6Fu,
        0x70u,
        0x69u,
        0x63u
    )
    private val packet = MQTTUnsubscribe(65u, listOf("test/topic/+", "/testtopic"))

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTTUnsubscribe.fromByteArray(2, array.copyOfRange(2, array.size))
        assertEquals(packet.packetIdentifier, result.packetIdentifier)
        assertEquals(packet.topicFilters[0], result.topicFilters[0])
        assertEquals(packet.topicFilters[1], result.topicFilters[1])
    }
}
