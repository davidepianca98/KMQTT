package mqtt.packets.mqttv5

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MQTTPubcompTest {

    private val array = ubyteArrayOf(0x70u, 0x04u, 0x00u, 0x41u, 0x00u, 0x00u)
    private val packet = MQTT5Pubcomp(65u)

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTT5Pubcomp.fromByteArray(0, array.copyOfRange(2, array.size))
        assertEquals(packet.packetId, result.packetId)
        assertEquals(packet.reasonCode, result.reasonCode)
    }
}
