package mqtt.broker.cluster.packets

import mqtt.packets.mqtt.MQTTPublish
import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream
import socket.streams.encodeVariableByteInteger

internal class SetRetainedPacket(val retained: Pair<MQTTPublish, String>) : ClusterPacket {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        // Type
        outStream.write(0.toUByte())

        // Options length
        outStream.encodeVariableByteInteger(0u)

        val publish = retained.first.toByteArray()
        // Data length
        outStream.encodeVariableByteInteger((publish.size + retained.second.length + 2).toUInt())

        // Data
        outStream.writeUTF8String(retained.second)
        outStream.write(publish)

        return outStream.toByteArray()
    }

    companion object : ClusterPacketDeserializer {

        override fun fromByteArray(data: UByteArray): SetRetainedPacket {
            val inStream = ByteArrayInputStream(data)

            val clientId = inStream.readUTF8String()
            val packetData = inStream.readRemaining()

            return SetRetainedPacket(Pair(MQTTPublish.fromByteArray(5, packetData), clientId)) // TODO version 4 also
        }
    }
}
