package mqtt.packets.mqttv5

import mqtt.packets.mqttv5.MQTT5Unsuback
import mqtt.packets.mqttv5.ReasonCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MQTTUnsubackTest {

    private val array = ubyteArrayOf(0xB0u, 0x05u, 0x00u, 0x41u, 0x00u, 0x00u, 0x11u)
    private val packet = MQTT5Unsuback(65u, listOf(ReasonCode.SUCCESS, ReasonCode.NO_SUBSCRIPTION_EXISTED))

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTT5Unsuback.fromByteArray(0, array.copyOfRange(2, array.size))
        assertEquals(packet.packetIdentifier, result.packetIdentifier)
        assertEquals(packet.reasonCodes[0], result.reasonCodes[0])
        assertEquals(packet.reasonCodes[1], result.reasonCodes[1])
    }
}
