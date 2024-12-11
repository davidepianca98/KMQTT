package io.github.davidepianca98.mqtt.packets.mqttv5

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MQTTDisconnectTest {

    private val array = ubyteArrayOf(0xE0u, 0x02u, 0x00u, 0x00u)
    private val packet = MQTT5Disconnect(ReasonCode.SUCCESS)

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTT5Disconnect.fromByteArray(0, array.copyOfRange(2, array.size))
        assertEquals(packet.reasonCode, result.reasonCode)
    }
}
