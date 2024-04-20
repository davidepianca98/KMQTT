package integration

import MQTTClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import mqtt.MQTTVersion
import mqtt.broker.Broker
import mqtt.broker.interfaces.Authentication
import mqtt.broker.interfaces.EnhancedAuthenticationProvider
import mqtt.packets.mqttv5.MQTT5Properties
import mqtt.packets.mqttv5.ReasonCode
import kotlin.test.Test

class AuthenticationTest {

    private suspend fun testAuthentication(
        client: MQTTClient,
        broker: Broker
    ) {
        var i = 0
        while (!client.isConnackReceived() && i < 1000) {
            broker.step()
            client.step()
            i++
            delay(10)
        }
        if (i >= 1000) {
            throw Exception("Test timeout")
        }
        client.disconnect(ReasonCode.SUCCESS)
    }

    @Test
    fun testSimpleAuthentication() = runTest {
        val broker = Broker(authentication = object : Authentication {
            override fun authenticate(clientId: String, username: String?, password: UByteArray?): Boolean {
                return username == "user" && password?.toByteArray()?.decodeToString() == "pass"
            }
        })
        broker.step()

        val client = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, null, userName = "user", password = "pass".encodeToByteArray().toUByteArray()) {}
        testAuthentication(client, broker)
        broker.stop()
    }

    /*@Test TODO test failing on JS
    fun testSimpleAuthenticationFailure() = runTest {
        val broker = Broker(authentication = object : Authentication {
            override fun authenticate(clientId: String, username: String?, password: UByteArray?): Boolean {
                return username == "user" && password?.toByteArray()?.decodeToString() == "pass"
            }
        })

        val client = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, null, userName = "user2", password = "pass".encodeToByteArray().toUByteArray()) {}

        assertFailsWith<MQTTException>(ReasonCode.NOT_AUTHORIZED.toString()) {
            testAuthentication(client, broker)
        }
        broker.stop()
    }*/

    @Test
    fun testEnhancedAuthentication() = runTest {
        val broker =
            Broker(enhancedAuthenticationProviders = mapOf("TEST-EN-AUTH" to object :
                EnhancedAuthenticationProvider {
                var counter = 0
                override fun authReceived(
                    clientId: String,
                    authenticationData: UByteArray?,
                    result: (completed: EnhancedAuthenticationProvider.Result, authenticationData: UByteArray?) -> Unit
                ) {
                    if (counter == 0) {
                        counter++
                        result(EnhancedAuthenticationProvider.Result.NEEDS_MORE, null)
                    } else {
                        result(EnhancedAuthenticationProvider.Result.SUCCESS, null)
                    }
                }
            }))

        val client = MQTTClient(MQTTVersion.MQTT5, "127.0.0.1", broker.port, null, properties = MQTT5Properties(authenticationMethod = "TEST-EN-AUTH"), enhancedAuthCallback = { null }) {}

        testAuthentication(client, broker)
        broker.stop()
    }
}
