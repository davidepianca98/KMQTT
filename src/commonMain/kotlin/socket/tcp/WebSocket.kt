package socket.tcp

import sha1
import socket.SocketInterface
import socket.streams.ByteArrayOutputStream
import socket.streams.DynamicByteBuffer
import socket.streams.EOFException
import toBase64


class WebSocket(private val socket: Socket) : SocketInterface {

    private var handshakeDone = false

    override fun send(data: UByteArray) {
        send(data, 0x2)
    }

    private fun send(data: UByteArray, opcode: Int) {
        val out = ByteArrayOutputStream()
        out.write(0x80.toUByte() or opcode.toUByte()) // fin

        if (data.size <= 125) {
            out.write(data.size.toUByte())
        } else if (data.size <= 65535) {
            out.write(126.toUByte())
            out.writeUShort(data.size.toUShort())
        } else {
            out.write(127.toUByte())
            out.writeULong(data.size.toULong())
        }
        out.write(data)

        socket.send(out.toByteArray())
    }

    override fun sendRemaining() {
        socket.sendRemaining()
    }

    private fun handshake(data: UByteArray) {
        val string = data.toByteArray().decodeToString()
        val get = Regex("^GET")

        if (get.find(string) != null) {
            val match1 = Regex("Sec-WebSocket-Protocol: (.*)", RegexOption.IGNORE_CASE)
            if (match1.find(string)?.groups?.get(1)?.value?.contains("mqtt") != true) {
                val response = "HTTP/1.1 400 Bad Request\r\n\r\n".encodeToByteArray().toUByteArray()
                socket.send(response)
                socket.close()
                throw IOException("mqtt not included in the subprotocols")
            }
            val match = Regex("Sec-WebSocket-Key: (.*)", RegexOption.IGNORE_CASE)
            val key = match.find(string)?.groups?.get(1)?.value
            val digest = (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").encodeToByteArray().sha1().toBase64()
            val response = (
                    "HTTP/1.1 101 Switching Protocols\r\n"
                            + "Connection: Upgrade\r\n"
                            + "Upgrade: websocket\r\n"
                            + "Sec-WebSocket-Accept: $digest\r\n"
                            + "Sec-WebSocket-Protocol: mqtt"
                            + "\r\n\r\n"
                    ).encodeToByteArray().toUByteArray()
            socket.send(response)

            handshakeDone = true
        } else {
            socket.close()
            throw IOException("Not a GET request")
        }
    }

    private val currentReceivedData = DynamicByteBuffer()

    private fun getLength(byte2: UByte): ULong {
        var payloadLen = (byte2 and 0x7Fu).toULong()
        if (payloadLen == 126.toULong()) {
            payloadLen = currentReceivedData.readUShort().toULong()
        } else if (payloadLen == 127.toULong()) {
            payloadLen = currentReceivedData.readULong()
        }
        return payloadLen
    }

    private fun decodeBinary(length: ULong, key: UByteArray): UByteArray {
        val decoded = ByteArrayOutputStream()
        for (i in 0 until length.toInt()) {
            decoded.write(currentReceivedData.read() xor key[i and 0x3])
        }
        currentReceivedData.shift()
        return decoded.toByteArray()
    }

    private fun decodeClose(length: ULong, key: UByteArray): UByteArray? {
        decodeBinary(length, key)
        send(UByteArray(0), 0x8)
        return null
    }

    private fun decodePing(length: ULong, key: UByteArray): UByteArray? {
        send(decodeBinary(length, key), 0xA)
        return null
    }

    private fun decode(data: UByteArray): UByteArray? {
        currentReceivedData.write(data)
        val out = ByteArrayOutputStream()
        try {
            while (true) {
                val byte1 = currentReceivedData.read()
                //val fin = (byte1 and 0x80u) == 0x80.toUByte()

                val opcode = byte1 and 0x0Fu

                val byte2 = currentReceivedData.read()
                val mask = (byte2 and 0x80u) == 0x80.toUByte()
                if (!mask) {
                    throw IOException("Mask must be set")
                }

                val length = getLength(byte2)
                val key = currentReceivedData.readBytes(4)

                val decoded = when (opcode.toInt()) {
                    0x2 -> decodeBinary(length, key)
                    0x8 -> decodeClose(length, key)
                    0x9 -> decodePing(length, key)
                    else -> {
                        close()
                        throw IOException("Opcode must be 0x2")
                    }
                }
                decoded?.let { out.write(decoded) }
            }
        } catch (e: EOFException) {
            currentReceivedData.clearReadCounter()
        }
        return if (out.size() == 0)
            null
        else
            out.toByteArray()
    }

    override fun read(): UByteArray? {
        val data = socket.read()
        if (data != null) {
            return if (!handshakeDone) {
                handshake(data)
                null
            } else {
                decode(data)
            }
        }
        return null
    }

    override fun close() {
        val out = ByteArrayOutputStream()
        out.write(0x88u) // fin and opcode 8
        try {
            socket.send(out.toByteArray())
        } catch (_: IOException) {

        }
        socket.close()
    }
}
