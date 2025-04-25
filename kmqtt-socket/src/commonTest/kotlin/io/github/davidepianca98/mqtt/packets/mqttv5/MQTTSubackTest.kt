package io.github.davidepianca98.mqtt.packets.mqttv5

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MQTTSubackTest {

    private val array = ubyteArrayOf(0x90u, 0x05u, 0x00u, 0x41u, 0x00u, 0x00u, 0x02u)
    private val packet = MQTT5Suback(65u, listOf(ReasonCode.SUCCESS, ReasonCode.GRANTED_QOS2))

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTT5Suback.fromByteArray(0, array.copyOfRange(2, array.size))
        assertEquals(packet.packetIdentifier, result.packetIdentifier)
        assertEquals(packet.reasonCodes[0], result.reasonCodes[0])
        assertEquals(packet.reasonCodes[1], result.reasonCodes[1])
    }
}
