package mqtt.packets.mqttv5

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MQTTPubrelTest {

    private val array = ubyteArrayOf(0x62u, 0x04u, 0x00u, 0x41u, 0x00u, 0x00u)
    private val packet = MQTTPubrel(65u)

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTTPubrel.fromByteArray(2, array.copyOfRange(2, array.size))
        assertEquals(packet.packetId, result.packetId)
    }
}
