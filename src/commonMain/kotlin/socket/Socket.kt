package socket

import socket.streams.InputStream
import socket.streams.OutputStream

expect class Socket {

    var soTimeout: Int

    fun close()
    fun getInputStream(): InputStream
    fun getOutputStream(): OutputStream
}

open class IOException(message: String? = null) : Exception(message)
class SocketTimeoutException : IOException()
