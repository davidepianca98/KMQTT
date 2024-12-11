package io.github.davidepianca98.mqtt.broker.cluster.packets

import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream
import io.github.davidepianca98.socket.streams.encodeVariableByteInteger

internal class RemoveSubscriptionPacket(val clientId: String, val topicFilter: String) : ClusterPacket {

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
