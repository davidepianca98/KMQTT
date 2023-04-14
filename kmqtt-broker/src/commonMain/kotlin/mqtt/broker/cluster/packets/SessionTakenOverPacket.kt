package mqtt.broker.cluster.packets

import socket.streams.ByteArrayInputStream
import socket.streams.ByteArrayOutputStream
import socket.streams.encodeVariableByteInteger

internal class SessionTakenOverPacket(val clientId: String) : ClusterPacket {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        // Type
        outStream.write(6.toUByte())

        // Options length
        outStream.encodeVariableByteInteger(0u)

        // Data length
        outStream.encodeVariableByteInteger(clientId.length.toUInt() + 2u)

        // Data
        outStream.writeUTF8String(clientId)

        return outStream.toByteArray()
    }

    companion object : ClusterPacketDeserializer {
        override fun fromByteArray(data: UByteArray): SessionTakenOverPacket {
            val inStream = ByteArrayInputStream(data)

            val clientId = inStream.readUTF8String()

            return SessionTakenOverPacket(clientId)
        }

    }
}
