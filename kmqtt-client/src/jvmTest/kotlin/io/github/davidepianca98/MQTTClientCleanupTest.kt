package io.github.davidepianca98

import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import io.github.davidepianca98.socket.IOException
import io.github.davidepianca98.socket.SocketInterface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MQTTClientCleanupTest {
    @Test
    fun sendRemainingFailureStopsClientAndPreservesException() {
        verifyStepFailure(FailingSocket(failOnFlush = true))
    }

    @Test
    fun readFailureStopsClientAndPreservesException() {
        verifyStepFailure(FailingSocket(failOnRead = true))
    }

    @Test
    fun disconnectSendFailureStillCleansUp() {
        val socket = FailingSocket(failOnSend = true)
        val client = clientWith(socket)
        assertSame(socket.failure, assertFailsWith<IOException> {
            client.disconnect(ReasonCode.SUCCESS)
        })
        assertFalse(client.isRunning())
        assertFalse(client.isConnackReceived())
        assertEquals(1, socket.closeCount)
        client.disconnect(ReasonCode.SUCCESS)
        assertEquals(1, socket.closeCount)
    }

    @Test
    fun protocolFailureIsNotReplacedByDisconnectSendFailure() {
        val socket = FailingSocket(
            failOnSend = true, readData = ubyteArrayOf(0u, 0u)
        )
        val client = clientWith(socket)
        val failure = assertFailsWith<io.github.davidepianca98.mqtt.MQTTException> {
            client.step()
        }
        assertSame(socket.failure, failure.suppressedExceptions.single())
        assertFalse(client.isRunning())
        assertEquals(1, socket.closeCount)
    }

    private fun verifyStepFailure(socket: FailingSocket) {
        val client = clientWith(socket)
        assertSame(socket.failure, assertFailsWith<IOException> { client.step() })
        assertFalse(client.isRunning())
        assertFalse(client.isConnackReceived())
        assertEquals(1, socket.closeCount)
        client.disconnect(ReasonCode.SUCCESS)
        assertEquals(1, socket.closeCount)
    }

    private fun clientWith(socket: SocketInterface): MQTTClient {
        val client = MQTTClient(
            MQTTVersion.MQTT3_1_1, "unused", 1883, null,
            autoInit = false, publishReceived = {}
        )
        MQTTClient::class.java.getDeclaredField("socket").apply {
            isAccessible = true
        }.set(client, socket)
        // init skips socket creation when one is already present.
        client.init()
        MQTTClient::class.java.getDeclaredMethod("handlePacket",
            io.github.davidepianca98.mqtt.packets.MQTTPacket::class.java).apply {
            isAccessible = true
        }.invoke(client, io.github.davidepianca98.mqtt.packets.mqttv4.MQTT4Connack(
            io.github.davidepianca98.mqtt.packets.ConnectAcknowledgeFlags(false),
            io.github.davidepianca98.mqtt.packets.mqttv4.ConnectReturnCode.CONNECTION_ACCEPTED
        ))
        return client
    }

    private class FailingSocket(
        private val failOnSend: Boolean = false,
        private val failOnFlush: Boolean = false,
        private val failOnRead: Boolean = false,
        private val readData: UByteArray? = null
    ) : SocketInterface {
        val failure = IOException("Transport failed")
        var closeCount = 0
        override fun send(data: UByteArray) {
            if (failOnSend) throw failure
        }
        override fun sendRemaining() {
            if (failOnFlush) throw failure
        }
        override fun read(): UByteArray? {
            if (failOnRead) throw failure
            return readData
        }
        override fun close() {
            closeCount++
        }
    }
}
