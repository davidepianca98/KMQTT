package integration

import MQTTClient
import mqtt.MQTTException
import mqtt.broker.Broker
import mqtt.broker.interfaces.Authentication
import mqtt.broker.interfaces.EnhancedAuthenticationProvider
import mqtt.packets.mqttv5.MQTT5Properties
import mqtt.packets.mqttv5.ReasonCode
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AuthenticationTest {

    private fun testAuthentication(
        client: MQTTClient,
        broker: Broker
    ) {
        var i = 0
        while (!client.connackReceived && i < 1000) {
            broker.step()
            client.step()
            i++
        }
        if (i >= 1000) {
            throw Exception("Test timeout")
        }
        client.disconnect(ReasonCode.SUCCESS)
    }

    @Test
    fun testSimpleAuthentication() {
        val broker = Broker(authentication = object : Authentication {
            override fun authenticate(clientId: String, username: String?, password: UByteArray?): Boolean {
                return username == "user" && password?.toByteArray()?.decodeToString() == "pass"
            }
        })
        broker.step()

        val client = MQTTClient(5, broker.host, broker.port, null, userName = "user", password = "pass".encodeToByteArray().toUByteArray()) {}
        testAuthentication(client, broker)
        broker.stop()
    }

    @Test
    fun testSimpleAuthenticationFailure() {
        val broker = Broker(authentication = object : Authentication {
            override fun authenticate(clientId: String, username: String?, password: UByteArray?): Boolean {
                return username == "user" && password?.toByteArray()?.decodeToString() == "pass"
            }
        })

        val client = MQTTClient(5, broker.host, broker.port, null, userName = "user2", password = "pass".encodeToByteArray().toUByteArray()) {}

        assertFailsWith<MQTTException>(ReasonCode.NOT_AUTHORIZED.toString()) {
            testAuthentication(client, broker)
        }
        broker.stop()
    }

    @Test
    fun testEnhancedAuthentication() {
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

        val client = MQTTClient(5, broker.host, broker.port, null, properties = MQTT5Properties(authenticationMethod = "TEST-EN-AUTH"), enhancedAuthCallback = { null }) {}

        testAuthentication(client, broker)
        broker.stop()
    }
}
