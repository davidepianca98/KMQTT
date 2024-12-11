package io.github.davidepianca98.socket.udp

internal expect class UDPSocket {

    fun send(data: UByteArray, address: String, port: Int)

    fun read(): UDPReadData?
}
