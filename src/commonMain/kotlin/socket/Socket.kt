package socket

expect class Socket {

    fun send(data: UByteArray)
}

open class IOException(message: String? = null) : Exception(message)
class SocketTimeoutException : IOException()
