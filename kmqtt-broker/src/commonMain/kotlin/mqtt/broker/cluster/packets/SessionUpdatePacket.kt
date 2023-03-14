package mqtt.broker.cluster.packets

import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream
import socket.streams.encodeVariableByteInteger

class SessionUpdatePacket(
    val clientId: String,
    val connected: Boolean,
    val sessionDisconnectedTimestamp: Long?,
    val sessionExpiryInterval: UInt
) : ClusterPacket {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        // Type
        outStream.write(5.toUByte())

        // Options length
        outStream.encodeVariableByteInteger(0u)

        // Data
        val dataOutStream = ByteArrayOutputStream()
        dataOutStream.writeUTF8String(clientId)

        dataOutStream.writeByte(if (connected) 1u else 0u)

        dataOutStream.writeUTF8String(sessionDisconnectedTimestamp?.toString(10) ?: "null")

        dataOutStream.write4BytesInt(sessionExpiryInterval)

        val data = dataOutStream.toByteArray()

        // Data length
        outStream.encodeVariableByteInteger(data.size.toUInt())

        outStream.write(data)
        return outStream.toByteArray()
    }

    companion object : ClusterPacketDeserializer {

        override fun fromByteArray(data: UByteArray): SessionUpdatePacket {
            val inStream = ByteArrayInputStream(data)

            val clientId = inStream.readUTF8String()
            val connected = inStream.read().toInt() == 1
            val sessionDisconnectedTimestampString = inStream.readUTF8String()
            val sessionDisconnectedTimestamp = if (sessionDisconnectedTimestampString == "null")
                null
            else
                sessionDisconnectedTimestampString.toLong()
            val sessionExpiryInterval = inStream.read4BytesInt()

            // TODO will and all other internal values

            return SessionUpdatePacket(clientId, connected, sessionDisconnectedTimestamp, sessionExpiryInterval)
        }

    }
}
