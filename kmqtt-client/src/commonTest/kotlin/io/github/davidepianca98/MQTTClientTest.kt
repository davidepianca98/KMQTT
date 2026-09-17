package io.github.davidepianca98

import io.github.davidepianca98.mqtt.MQTTVersion
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MQTTClientTest {

    @Test
    fun testNonPositiveSocketBufferSizeThrows() {
        assertFailsWith<IllegalArgumentException> {
            MQTTClient(
                mqttVersion = MQTTVersion.MQTT5,
                address = "127.0.0.1",
                port = 1883,
                tls = null,
                socketBufferSize = 0,
                autoInit = false,
                publishReceived = {},
            )
        }
    }
}
