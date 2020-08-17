package mqtt.packets.mqttv5

import mqtt.packets.ConnectAcknowledgeFlags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MQTTConnackTest {

    private val array = ubyteArrayOf(
        0x20u,
        0x29u,
        0x00u,
        0x00u,
        0x26u,
        0x12u,
        0x00u,
        0x1Eu,
        0x78u,
        0x74u,
        0x71u,
        0x71u,
        0x77u,
        0x6Fu,
        0x72u,
        0x6Au,
        0x6Au,
        0x6Eu,
        0x68u,
        0x66u,
        0x73u,
        0x6Bu,
        0x77u,
        0x78u,
        0x65u,
        0x61u,
        0x74u,
        0x6Fu,
        0x6Du,
        0x79u,
        0x66u,
        0x72u,
        0x63u,
        0x6du,
        0x6eu,
        0x6au,
        0x65u,
        0x66u,
        0x27u,
        0x00u,
        0x00u,
        0x80u,
        0x00u
    )
    private val packet = MQTT5Connack(
        ConnectAcknowledgeFlags(false),
        ReasonCode.SUCCESS,
        MQTT5Properties().apply {
            assignedClientIdentifier = "xtqqworjjnhfskwxeatomyfrcmnjef"
            maximumPacketSize = 32768u
        }
    )

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTT5Connack.fromByteArray(0, array.copyOfRange(2, array.size))
        assertEquals(packet.connectAcknowledgeFlags.sessionPresentFlag, result.connectAcknowledgeFlags.sessionPresentFlag)
        assertEquals(packet.connectReasonCode, result.connectReasonCode)
    }
}
