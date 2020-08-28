package mqtt.broker.cluster.packets

import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream
import socket.streams.encodeVariableByteInteger

class RemoveSubscriptionPacket(val clientId: String, val topicFilter: String) : ClusterPacket {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        // Type
        outStream.write(2.toUByte())

        // Options length
        outStream.encodeVariableByteInteger(0u)


        val dataOutStream = ByteArrayOutputStream()
        dataOutStream.writeUTF8String(clientId)
        dataOutStream.writeUTF8String(topicFilter)

        val data = dataOutStream.toByteArray()

        outStream.encodeVariableByteInteger(data.size.toUInt())
        outStream.write(data)
        return outStream.toByteArray()
    }

    companion object : ClusterPacketDeserializer {

        override fun fromByteArray(data: UByteArray): RemoveSubscriptionPacket {
            val inStream = ByteArrayInputStream(data)

            val clientId = inStream.readUTF8String()

            val topicFilter = inStream.readUTF8String()

            return RemoveSubscriptionPacket(clientId, topicFilter)
        }

    }
}
