package mqtt.packets.mqttv5

import kotlin.test.Test
import kotlin.test.assertTrue

class MQTTPingreqTest {

    private val array = ubyteArrayOf(0xC0u, 0x00u)
    private val packet = MQTTPingreq()

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        MQTTPingreq.fromByteArray(0, array.copyOfRange(2, array.size))
    }
}
