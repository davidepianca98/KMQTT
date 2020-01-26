package mqtt

import mqtt.packets.MQTTSerializer
import socket.streams.OutputStream

class MQTTOutputStream(private val outputStream: OutputStream) : OutputStream {

    suspend fun writePacket(packet: MQTTSerializer) {
        write(packet.toByteArray().toUByteArray())
    }

    override suspend fun write(b: UByte) {
        outputStream.write(b)
    }

    override suspend fun write(b: UByteArray) {
        outputStream.write(b)
    }
}
