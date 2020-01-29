package mqtt.packets.mqttv5

import toHexString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMQTTPuback {

    private val array = ubyteArrayOf(0x40u, 0x04u, 0x00u, 0x41u, 0x00u, 0x00u)
    private val packet = MQTTPuback(65u)

    @Test
    fun testToByteArray() {
        println(packet.toByteArray().toHexString())
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTTPuback.fromByteArray(0, array.copyOfRange(2, array.size))
        assertEquals(packet.packetId, result.packetId)
        assertEquals(packet.reasonCode, result.reasonCode)
    }
}
