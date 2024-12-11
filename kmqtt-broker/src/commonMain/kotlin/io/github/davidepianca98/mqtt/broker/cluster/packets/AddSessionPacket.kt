package io.github.davidepianca98.mqtt.broker.cluster.packets

import io.github.davidepianca98.socket.streams.ByteArrayInputStream
import io.github.davidepianca98.socket.streams.ByteArrayOutputStream
import io.github.davidepianca98.socket.streams.encodeVariableByteInteger

internal class AddSessionPacket(val clientId: String, val sessionExpiryInterval: UInt) : ClusterPacket {

    override fun toByteArray(): UByteArray {
        val outStream = ByteArrayOutputStream()

        // Type
        outStream.write(4.toUByte())

        // Options length
        outStream.encodeVariableByteInteger(0u)

        // Data
        val dataOutStream = ByteArrayOutputStream()
        dataOutStream.writeUTF8String(clientId)
        dataOutStream.write4BytesInt(sessionExpiryInterval)

        val data = dataOutStream.toByteArray()
        // Data length
        outStream.encodeVariableByteInteger(data.size.toUInt())

        outStream.write(data)

        return outStream.toByteArray()
    }

    companion object : ClusterPacketDeserializer {

        override fun fromByteArray(data: UByteArray): AddSessionPacket {
            val inStream = ByteArrayInputStream(data)

            val clientId = inStream.readUTF8String()
            val sessionExpiryInterval = inStream.read4BytesInt()

            return AddSessionPacket(clientId, sessionExpiryInterval)
        }

    }
}
