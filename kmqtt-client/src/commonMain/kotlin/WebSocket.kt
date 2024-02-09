import socket.IOException
import socket.SocketInterface
import socket.streams.ByteArrayOutputStream
import socket.streams.DynamicByteBuffer
import socket.streams.EOFException
import kotlin.random.Random

public class WebSocket(private val socket: SocketInterface, host: String, path: String = "/mqtt") : SocketInterface {

    private var getSent = false
    private var handshakeDone = false

    private val pendingMessages = mutableListOf<UByteArray>()

    private val key = Random.Default.nextBytes(16).toBase64()

    private val handshakeMessage = "GET $path HTTP/1.1\r\n" +
            "Host: $host\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Key: $key\r\n" +
            "Sec-WebSocket-Protocol: mqtt\r\n" +
            "Sec-WebSocket-Version: 13\r\n\r\n"

    override fun send(data: UByteArray) {
        if (!getSent) {
            socket.send(handshakeMessage.encodeToByteArray().toUByteArray())
            getSent = true
            pendingMessages.add(data)
        } else {
            if (!handshakeDone) {
                pendingMessages.add(data)
            } else {
                send(data, 0x2)
            }
        }
    }

    private fun send(data: UByteArray, opcode: Int) {
        val out = ByteArrayOutputStream()
        out.write(0x80.toUByte() or opcode.toUByte()) // fin

        if (data.size <= 125) {
            out.write(data.size.toUByte() or 0x80u)
        } else if (data.size <= 65535) {
            out.write(126.toUByte() or 0x80u)
            out.writeUShort(data.size.toUShort())
        } else {
            out.write(127.toUByte() or 0x80u)
            out.writeULong(data.size.toULong())
        }
        val maskingKey = Random.Default.nextBytes(4).toUByteArray()
        out.write(maskingKey)
        // Mask data
        for (i in data.indices) {
            out.write(data[i] xor maskingKey[i and 0x3])
        }

        socket.send(out.toByteArray())
    }

    override fun sendRemaining() {
        socket.sendRemaining()
    }

    private fun handshake(data: UByteArray) {
        val string = data.toByteArray().decodeToString()
        val get = Regex("^HTTP/1.1 101 Switching Protocols")

        if (get.find(string) != null) {
            val matchUpgrade = Regex("Upgrade: (.*)", RegexOption.IGNORE_CASE)
            if (matchUpgrade.find(string)?.groups?.get(1)?.value?.contains("websocket", ignoreCase = true) != true) {
                socket.close()
                throw IOException("Connection not upgraded to WebSocket")
            }
            val matchConnection = Regex("Connection: (.*)", RegexOption.IGNORE_CASE)
            if (matchConnection.find(string)?.groups?.get(1)?.value?.contains("Upgrade", ignoreCase = true) != true) {
                socket.close()
                throw IOException("Connection not upgraded to WebSocket")
            }
            val matchWsProtocol = Regex("Sec-WebSocket-Protocol: (.*)", RegexOption.IGNORE_CASE)
            if (matchWsProtocol.find(string)?.groups?.get(1)?.value?.contains("mqtt", ignoreCase = true) != true) {
                socket.close()
                throw IOException("mqtt not included in the subprotocols")
            }
            val matchWsAccept = Regex("Sec-WebSocket-Accept: (.*)", RegexOption.IGNORE_CASE)
            val accept = matchWsAccept.find(string)?.groups?.get(1)?.value
            val digest = (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").encodeToByteArray().sha1().toBase64()
            if (accept != digest) {
                throw IOException("Wrong websocket Accept received")
            }
            handshakeDone = true

            val iterator = pendingMessages.iterator()
            while (iterator.hasNext()) {
                send(iterator.next())
                iterator.remove()
            }
        } else {
            socket.close()
            throw IOException("Not a HTTP 101")
        }
    }

    public fun ping() {
        send(UByteArray(0), 0x9)
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

    private fun decodeBinary(length: ULong): UByteArray {
        val decoded = currentReceivedData.readBytes(length.toInt())
        currentReceivedData.shift()
        return decoded
    }

    private fun decodeClose(length: ULong): UByteArray? {
        decodeBinary(length)
        close()
        return null
    }

    private fun decodePong(length: ULong): UByteArray? {
        decodeBinary(length)
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
                if (mask) {
                    throw IOException("Mask must not be set")
                }

                val length = getLength(byte2)

                val decoded = when (opcode.toInt()) {
                    0x2 -> decodeBinary(length)
                    0x8 -> decodeClose(length)
                    0xA -> decodePong(length)
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
        send(ubyteArrayOf(0x03u, 0xe8u), 0x8)
        socket.close()
    }
}
