package socket

import socket.streams.EOFException
import socket.streams.InputStream
import socket.streams.OutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

@ExperimentalUnsignedTypes
actual class Socket(private val socket: Socket) {
    actual var soTimeout: Int
        get() = socket.soTimeout
        set(value) {
            socket.soTimeout = value
        }

    private val dataInputStream = DataInputStream(socket.getInputStream())
    private val dataOutputStream = DataOutputStream(socket.getOutputStream())

    private val inputStream = object : InputStream {
        override fun read(): UByte {
            var result: UByte? = null
            tryJavaSocket {
                result = dataInputStream.read().toUByte()
            }
            return result ?: throw Exception()
        }

        override fun readBytes(length: Int): UByteArray {
            val data = ByteArray(length)
            tryJavaSocket {
                dataInputStream.read(data, 0, length)
            }
            return data.toUByteArray()
        }
    }

    private val outputStream = object : OutputStream {
        override fun write(b: UByte) {
            tryJavaSocket {
                dataOutputStream.write(b.toInt())
                dataOutputStream.flush()
            }
        }

        override fun write(b: UByteArray) {
            tryJavaSocket {
                dataOutputStream.write(b.toByteArray())
                dataOutputStream.flush()
            }
        }
    }

    fun tryJavaSocket(block: () -> Unit) {
        try {
            block()
        } catch (e: java.net.SocketTimeoutException) {
            throw SocketTimeoutException()
        } catch (e: java.io.EOFException) {
            throw EOFException()
        } catch (e: java.io.IOException) {
            throw IOException(e.message)
        } catch (e: Exception) {
            throw e
        }
    }

    actual fun close() {
        socket.close()
    }

    actual fun getInputStream(): InputStream {
        return inputStream
    }

    actual fun getOutputStream(): OutputStream {
        return outputStream
    }

}
