package mqtt.packets.mqttv5

import mqtt.packets.Qos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMQTTPublish {

    private val array = ubyteArrayOf(
        0x34u,
        0x13u,
        0x00u,
        0x0Au,
        0x74u,
        0x65u,
        0x73u,
        0x74u,
        0x2Fu,
        0x74u,
        0x6fu,
        0x70u,
        0x69u,
        0x63u,
        0x02u,
        0x37u,
        0x00u,
        0x01u,
        0x02u,
        0x03u,
        0x04u
    )
    private val packet = MQTTPublish(
        false,
        Qos.EXACTLY_ONCE,
        false,
        "test/topic",
        567u,
        payload = ubyteArrayOf(0x01u, 0x02u, 0x03u, 0x04u)
    )

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTTPublish.fromByteArray(4, array.copyOfRange(2, array.size))
        assertEquals(packet.packetId, result.packetId)
        assertEquals(packet.qos, result.qos)
        assertEquals(packet.topicName, result.topicName)
        assertTrue(packet.payload!!.contentEquals(ubyteArrayOf(0x01u, 0x02u, 0x03u, 0x04u)))
    }
}
