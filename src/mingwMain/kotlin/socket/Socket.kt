package socket

import kotlinx.cinterop.*
import platform.posix.SD_SEND
import platform.posix.SOCKET_ERROR
import platform.windows.*
import socket.streams.EOFException
import socket.streams.InputStream
import socket.streams.OutputStream

actual class Socket(private val socket: ULong) {

    actual var soTimeout: Int
        get() {
            memScoped {
                val result = allocArray<ByteVar>(4)
                val resultLength = allocArray<IntVar>(1)
                getsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, result, resultLength)
                return result.reinterpret<IntVar>()[0]
            }
        }
        set(value) {
            setsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, value.toString(), 4)
        }

    private val inputStream = object : InputStream {

        private val buf = ByteArray(8192)
        private var bufSize = 0
        private var position = 0

        private fun read(length: Int): ByteArray {
            if (position + length > bufSize) {
                memScoped {
                    buf.usePinned { pinned ->
                        bufSize = recv(socket, pinned.addressOf(0), buf.size, 0)
                        when {
                            bufSize == 0 -> throw EOFException()
                            bufSize > 0 -> position = 0
                            else -> {
                                val error = WSAGetLastError()
                                if (error == WSAETIMEDOUT)
                                    throw SocketTimeoutException()
                                else
                                    throw IOException("Error in recv $error")
                            }
                        }
                    }
                }
            }
            val array = buf.copyOfRange(position, position + length)
            position += length
            return array
        }

        override fun read(): UByte {
            return read(1)[0].toUByte()
        }

        override fun readBytes(length: Int): UByteArray {
            return read(length).toUByteArray()
        }
    }

    private val outputStream = object : OutputStream {
        override fun write(b: UByte) {
            val array = UByteArray(1)
            array[0] = b
            write(array)
        }

        override fun write(b: UByteArray) {
            b.toByteArray().usePinned { pinned ->
                if (platform.posix.send(socket, pinned.addressOf(0), b.size, 0) == SOCKET_ERROR) {
                    throw IOException("Error in send ${WSAGetLastError()}")
                }
            }
        }
    }

    actual fun close() {
        shutdown(socket, SD_SEND)
        closesocket(socket)
    }

    actual fun getInputStream(): InputStream {
        return inputStream
    }

    actual fun getOutputStream(): OutputStream {
        return outputStream
    }

}
