package io.github.davidepianca98.integration

import io.github.davidepianca98.mqtt.broker.Broker
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BrokerConstructorTest {

    @Test
    fun testNonPositiveSocketBufferSizeThrows() {
        assertFailsWith<IllegalArgumentException> {
            Broker(socketBufferSize = 0)
        }
    }

    @Test
    fun testUdpStillLimitedByMaximumPacketSize() {
        assertFailsWith<IllegalArgumentException> {
            Broker(enableUdp = true, maximumPacketSize = 65536u)
        }
    }
}
