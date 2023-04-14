package mqtt.broker.cluster.packets

import mqtt.packets.mqtt.MQTTPublish
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream
import socket.streams.encodeVariableByteInteger

internal class PublishPacket(val packet: MQTTPublish) : ClusterPacket {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        // Type
        outStream.write(3.toUByte())

        // Options length
        outStream.encodeVariableByteInteger(0u)

        val publish = packet.toByteArray()
        // Data length
        outStream.encodeVariableByteInteger(publish.size.toUInt())

        // Data
        outStream.write(publish)

        return outStream.toByteArray()
    }

    companion object : ClusterPacketDeserializer {

        override fun fromByteArray(data: UByteArray): PublishPacket {
            val inStream = ByteArrayInputStream(data)

            val packetData = inStream.readRemaining()

            return PublishPacket(MQTTPublish.fromByteArray(5, packetData)) // TODO version 4 also
        }

    }
}
