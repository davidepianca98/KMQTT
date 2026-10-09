package io.github.davidepianca98

import io.github.davidepianca98.socket.SocketClosedException
import io.github.davidepianca98.socket.IOException
import io.github.davidepianca98.socket.tls.TLSClientSettings
import io.github.davidepianca98.socket.tls.TLSEngine
import io.github.davidepianca98.socket.tls.TLSSocket
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.convert
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import platform.posix.AF_UNIX
import platform.posix.F_GETFD
import platform.posix.SOCK_STREAM
import platform.posix.dup2
import platform.posix.fcntl
import platform.posix.socketpair

class TLSCloseTest {
    @Test
    fun engineCloseIsIdempotentAndRejectsFurtherUse() {
        val engine = TLSClientEngine(TLSClientSettings(checkServerCertificate = false))
        engine.close()
        engine.close()
        assertFailsWith<SocketClosedException> { engine.isInitFinished }
        assertFailsWith<SocketClosedException> { engine.bioShouldRetry }
        assertFailsWith<SocketClosedException> { engine.getError(-1) }
        memScoped {
            val buffer = allocArray<ByteVar>(1)
            assertFailsWith<SocketClosedException> { engine.read(buffer, 1) }
            assertFailsWith<SocketClosedException> { engine.write(buffer, 1) }
            assertFailsWith<SocketClosedException> { engine.bioRead(buffer, 1) }
            assertFailsWith<SocketClosedException> { engine.bioWrite(buffer, 1) }
        }
    }

    @Test
    fun peerCloseThenCallerCloseDoesNotFreeEngineOrDescriptorTwice(): Unit = memScoped {
        val descriptors = allocArray<kotlinx.cinterop.IntVar>(2)
        assertEquals(0, socketpair(AF_UNIX, SOCK_STREAM, 0, descriptors))
        val descriptor = descriptors[0]
        val peer = descriptors[1]
        val engine = CountingEngine()
        val socket = TLSSocket(descriptor, engine, null, ByteArray(16))
        try {
            assertEquals(0, platform.posix.shutdown(peer, platform.posix.SHUT_WR))
            assertFailsWith<SocketClosedException> { socket.read() }
            assertEquals(1, engine.closeCount)

            // Reuse the descriptor to detect an accidental second native close.
            assertEquals(descriptor, dup2(peer, descriptor))
            socket.close()
            assertEquals(1, engine.closeCount)
            assertTrue(fcntl(descriptor, F_GETFD) >= 0)
            assertFailsWith<SocketClosedException> { socket.read() }
            assertFailsWith<SocketClosedException> { socket.send(ubyteArrayOf(1u)) }
            assertFailsWith<SocketClosedException> { socket.sendRemaining() }
        } finally {
            socket.close()
            platform.posix.close(descriptor)
            platform.posix.close(peer)
        }
    }

    @Test
    fun tlsErrorThenCallerCloseOnlyFreesEngineOnce(): Unit = memScoped {
        val descriptors = allocArray<kotlinx.cinterop.IntVar>(2)
        assertEquals(0, socketpair(AF_UNIX, SOCK_STREAM, 0, descriptors))
        val engine = CountingEngine(failRead = true)
        val socket = TLSSocket(descriptors[0], engine, null, ByteArray(16))
        try {
            val data = allocArray<ByteVar>(1)
            data[0] = 0
            assertEquals(1L, platform.posix.write(descriptors[1], data, 1.convert()))
            val failure = assertFailsWith<IOException> { socket.read() }
            assertEquals("OpenSSL error 1", failure.message)
            socket.close()
            assertEquals(1, engine.closeCount)
        } finally {
            socket.close()
            platform.posix.close(descriptors[1])
        }
    }

    private class CountingEngine(private val failRead: Boolean = false) : TLSEngine {
        var closeCount = 0
        override val isInitFinished: Boolean = false
        override val bioShouldRetry: Boolean = false
        override fun write(buffer: CPointer<ByteVar>, length: Int): Int = error("Unexpected write")
        override fun read(buffer: CPointer<ByteVar>, length: Int): Int =
            if (failRead) -1 else error("Unexpected read")
        override fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int =
            if (failRead) -1 else error("Unexpected BIO read")
        override fun bioWrite(buffer: CPointer<ByteVar>, length: Int): Int =
            if (failRead) length else error("Unexpected BIO write")
        override fun getError(result: Int): Int =
            if (failRead) 1 else error("Unexpected error query")
        override fun close() {
            closeCount++
        }
    }
}
