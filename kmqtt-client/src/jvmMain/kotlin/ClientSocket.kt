import socket.tcp.Socket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

actual class ClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int
) : Socket(
    SocketChannel.open(),
    null,
    ByteBuffer.allocate(maximumPacketSize),
    ByteBuffer.allocate(maximumPacketSize)
) {

    init {
        channel.connect(InetSocketAddress(address, port))
    }
}