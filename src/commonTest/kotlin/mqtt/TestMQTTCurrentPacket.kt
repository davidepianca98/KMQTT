package mqtt

import mqtt.packets.mqttv5.MQTTPublish
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMQTTCurrentPacket {

    @Test
    fun testCurrentPacketSinglePublish() {
        val currentPacket = MQTTCurrentPacket(32000u)

        val publish = MQTTPublish(false, topicName = "test/topic", packetId = null, payload = UByteArray(60))
        val data = publish.toByteArray()

        val packets = currentPacket.addData(data)

        assertEquals(1, packets.size)
        assertEquals(publish.topicName, (packets[0] as MQTTPublish).topicName)
        assertEquals(publish.payload!!.size, (packets[0] as MQTTPublish).payload!!.size)
    }

    @Test
    fun testCurrentPacket3Publish() {
        val currentPacket = MQTTCurrentPacket(32000u)

        val publish1 = MQTTPublish(false, topicName = "test/topic1", packetId = null, payload = UByteArray(60))
        val publish2 = MQTTPublish(false, topicName = "test/topic2", packetId = null, payload = UByteArray(100))
        val publish3 = MQTTPublish(false, topicName = "test/topic3", packetId = null, payload = UByteArray(50))
        val data = publish1.toByteArray() + publish2.toByteArray() + publish3.toByteArray()

        val packets = currentPacket.addData(data)

        assertEquals(3, packets.size)
        assertEquals(publish1.topicName, (packets[0] as MQTTPublish).topicName)
        assertEquals(publish1.payload!!.size, (packets[0] as MQTTPublish).payload!!.size)
        assertEquals(publish2.topicName, (packets[1] as MQTTPublish).topicName)
        assertEquals(publish2.payload!!.size, (packets[1] as MQTTPublish).payload!!.size)
        assertEquals(publish3.topicName, (packets[2] as MQTTPublish).topicName)
        assertEquals(publish3.payload!!.size, (packets[2] as MQTTPublish).payload!!.size)
    }

    @Test
    fun testCurrentPacket3PublishSplit() {
        val currentPacket = MQTTCurrentPacket(32000u)

        val publish1 = MQTTPublish(false, topicName = "test/topic1", packetId = null, payload = UByteArray(60))
        val publish2 = MQTTPublish(false, topicName = "test/topic2", packetId = null, payload = UByteArray(100))
        val publish3 = MQTTPublish(false, topicName = "test/topic3", packetId = null, payload = UByteArray(50))
        val data = publish1.toByteArray() + publish2.toByteArray().copyOfRange(0, 23)

        val packets = currentPacket.addData(data)

        assertEquals(1, packets.size)
        assertEquals(publish1.topicName, (packets[0] as MQTTPublish).topicName)
        assertEquals(publish1.payload!!.size, (packets[0] as MQTTPublish).payload!!.size)

        val dataSecondRound =
            publish2.toByteArray().copyOfRange(23, publish2.toByteArray().size) + publish3.toByteArray()
        val packetsSecondRound = currentPacket.addData(dataSecondRound)
        assertEquals(2, packetsSecondRound.size)
        assertEquals(publish2.topicName, (packetsSecondRound[0] as MQTTPublish).topicName)
        assertEquals(publish2.payload!!.size, (packetsSecondRound[0] as MQTTPublish).payload!!.size)
        assertEquals(publish3.topicName, (packetsSecondRound[1] as MQTTPublish).topicName)
        assertEquals(publish3.payload!!.size, (packetsSecondRound[1] as MQTTPublish).payload!!.size)
    }
}
