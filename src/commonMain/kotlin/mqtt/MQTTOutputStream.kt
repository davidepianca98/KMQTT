package mqtt

import mqtt.packets.MQTTSerializer
import socket.streams.OutputStream

class MQTTOutputStream(private val outputStream: OutputStream) : OutputStream {

    fun writePacket(packet: MQTTSerializer) {
        write(packet.toByteArray().toUByteArray())
    }

    override fun write(b: UByte) {
        outputStream.write(b)
    }

    override fun write(b: UByteArray) {
        outputStream.write(b)
    }
}
