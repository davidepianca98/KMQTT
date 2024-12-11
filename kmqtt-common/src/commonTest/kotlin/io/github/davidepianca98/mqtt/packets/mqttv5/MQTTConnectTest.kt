package io.github.davidepianca98.mqtt.packets.mqttv5

import io.github.davidepianca98.mqtt.packets.ConnectFlags
import io.github.davidepianca98.mqtt.packets.Qos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MQTTConnectTest {

    private val array = ubyteArrayOf(
        0x10u,
        0x39u,
        0x00u,
        0x04u,
        0x4du,
        0x51u,
        0x54u,
        0x54u,
        0x05u,
        0x16u,
        0x00u,
        0x3cu,
        0x05u,
        0x11u,
        0x00u,
        0x00u,
        0x00u,
        0x00u,
        0x00u,
        0x0eu,
        0x6du,
        0x71u,
        0x74u,
        0x74u,
        0x78u,
        0x5fu,
        0x66u,
        0x35u,
        0x61u,
        0x63u,
        0x30u,
        0x31u,
        0x37u,
        0x31u,
        0x06u,
        0x03u,
        0x00u,
        0x00u,
        0x08u,
        0x00u,
        0x00u,
        0x00u,
        0x0au,
        0x2fu,
        0x74u,
        0x65u,
        0x73u,
        0x74u,
        0x2fu,
        0x77u,
        0x69u,
        0x6cu,
        0x6cu,
        0x00u,
        0x04u,
        0x61u,
        0x62u,
        0x63u,
        0x64u
    )
    private val packet = MQTT5Connect(
        "MQTT",
        ConnectFlags(
            false,
            false,
            false,
            Qos.EXACTLY_ONCE,
            true,
            true,
            false
        ),
        60,
        clientID = "mqttx_f5ac0171",
        properties = MQTT5Properties(sessionExpiryInterval = 0u),
        willTopic = "/test/will",
        willPayload = "abcd".encodeToByteArray().toUByteArray(),
        willProperties = MQTT5Properties(contentType = "", responseTopic = "")
    )

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTT5Connect.fromByteArray(0, array.copyOfRange(2, array.size))
        assertEquals(packet.protocolName, result.protocolName)
        assertEquals(packet.protocolVersion, result.protocolVersion)
        assertEquals(packet.keepAlive, result.keepAlive)
        assertEquals(packet.connectFlags.willFlag, true)
        assertEquals(packet.connectFlags.willQos, Qos.EXACTLY_ONCE)
    }
}
