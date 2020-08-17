package mqtt.packets.mqttv5

import kotlin.test.Test
import kotlin.test.assertTrue

class MQTTPingrespTest {

    private val array = ubyteArrayOf(0xD0u, 0x00u)
    private val packet = MQTT5Pingresp()

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        MQTT5Pingresp.fromByteArray(0, array.copyOfRange(2, array.size))
    }
}
