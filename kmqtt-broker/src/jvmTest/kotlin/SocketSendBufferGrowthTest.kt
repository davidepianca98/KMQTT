import io.github.davidepianca98.socket.tcp.Socket
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SocketSendBufferGrowthTest {

    @Test
    fun testSendPreservesPendingBytesWhenGrowingOnOverflow() {
        val serverChannel = ServerSocketChannel.open()
        serverChannel.bind(InetSocketAddress("127.0.0.1", 0))

        val clientChannel = SocketChannel.open()
        // Small kernel send buffer so a large payload triggers a short write,
        // leaving unflushed bytes pending in the app-level send buffer.
        clientChannel.setOption(StandardSocketOptions.SO_SNDBUF, 8 * 1024)
        clientChannel.connect(InetSocketAddress("127.0.0.1", serverChannel.socket().localPort))
        clientChannel.configureBlocking(false)

        val peerChannel = serverChannel.accept()
        serverChannel.close()
        peerChannel.configureBlocking(false)

        val socket = Socket(clientChannel, null, ByteBuffer.allocate(16 * 1024), ByteBuffer.allocate(16 * 1024))

        val payloadA = UByteArray(256 * 1024) { (it % 251).toUByte() }
        val payloadB = UByteArray(256 * 1024) { ((it + 7) % 241).toUByte() }

        // Nobody is reading on the peer yet, so this write is short: part of
        // payloadA stays pending in the socket's app-level send buffer.
        socket.send(payloadA)
        // This must overflow the 16 KiB buffer while bytes are still pending;
        // with the bug, those pending bytes are silently dropped here.
        socket.send(payloadB)

        val expected = (payloadA + payloadB).toByteArray()
        val totalSize = expected.size
        val received = ByteBuffer.allocate(totalSize)
        val deadline = System.currentTimeMillis() + 10_000

        try {
            while (received.hasRemaining() && System.currentTimeMillis() < deadline) {
                socket.sendRemaining()
                if (peerChannel.read(received) == 0) {
                    Thread.sleep(1)
                }
            }
        } finally {
            peerChannel.close()
            socket.close()
        }

        assertEquals(totalSize, received.position(), "receiver got fewer bytes than sent within deadline")
        assertContentEquals(expected, received.array())
    }
}
