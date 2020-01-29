package mqtt.packets

import mqtt.packets.mqttv5.MQTTAuth
import mqtt.packets.mqttv5.MQTTProperties
import mqtt.packets.mqttv5.ReasonCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMQTTAuth {

    private val array = ubyteArrayOf(0xF0u, 0x12u, 0x18u, 0x10u, 0x15u, 0x00u, 0x06u, 0x4Du, 0x45u, 0x54u, 0x48u, 0x4Fu, 0x44u, 0x16u, 0x00u, 0x04u, 0x01u, 0x02u, 0x03u, 0x04u)
    private val packet = MQTTAuth(
        ReasonCode.CONTINUE_AUTHENTICATION,
        MQTTProperties().apply {
            authenticationMethod = "METHOD"
            authenticationData = ubyteArrayOf(0x01u, 0x02u, 0x03u, 0x04u)
        }
    )

    @Test
    fun testToByteArray() {
        assertTrue(array.contentEquals(packet.toByteArray()))
    }

    @Test
    fun testFromByteArray() {
        val result = MQTTAuth.fromByteArray(0, array.copyOfRange(2, array.size))
        assertEquals(packet.authenticateReasonCode, result.authenticateReasonCode)
        assertEquals(packet.properties.authenticationMethod, result.properties.authenticationMethod)
        assertTrue(packet.properties.authenticationData!!.contentEquals(result.properties.authenticationData!!))
    }
}
