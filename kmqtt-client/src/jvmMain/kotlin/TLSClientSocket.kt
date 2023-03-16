import socket.tls.TLSSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import javax.net.ssl.SSLContext

actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    private val readTimeOut: Int
) : TLSSocket(
    SocketChannel.open(InetSocketAddress(address, port)).apply {
        configureBlocking(false)
    },
    null,
    ByteBuffer.allocate(maximumPacketSize),
    ByteBuffer.allocate(maximumPacketSize),
    ByteBuffer.allocate(maximumPacketSize),
    ByteBuffer.allocate(maximumPacketSize),
    SSLContext.getDefault().createSSLEngine().apply {
        useClientMode = true
    }
) {
    private val selector = Selector.open()

    init {
        channel.register(selector, SelectionKey.OP_READ)
    }

    override fun read(): UByteArray? {
        val count = selector.select(readTimeOut.toLong())
        return if (count > 0) {
            selector.selectedKeys().clear()
            super.read()
        } else {
            null
        }
    }
}
