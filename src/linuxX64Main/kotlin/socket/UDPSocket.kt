package socket

actual class UDPSocket {
    actual fun send(data: UByteArray, address: String, port: Int) {
    }

    actual fun read(): UDPReadData? {
        TODO("Not yet implemented")
    }

}
