package integration

import com.hivemq.client.mqtt.datatypes.MqttUtf8String
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientConfig
import com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5ConnAckException
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5Auth
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5AuthBuilder
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5EnhancedAuthBuilder
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAckReasonCode
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5Disconnect
import mqtt.broker.Authentication
import mqtt.broker.Broker
import mqtt.broker.EnhancedAuthenticationProvider
import org.junit.Test
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthenticationTest {

    private fun testAuthentication(
        client: Mqtt5BlockingClient,
        broker: Broker,
        expectedReasonCode: Mqtt5ConnAckReasonCode
    ) {
        brokerTest(broker) {
            val connack = client.connect()
            assertEquals(expectedReasonCode, connack.reasonCode)
            client.disconnect()
        }
    }

    @Test
    fun testSimpleAuthentication() {
        val broker = Broker(authentication = object : Authentication {
            override fun authenticate(username: String?, password: UByteArray?): Boolean {
                return username == "user" && password?.toByteArray()?.decodeToString() == "pass"
            }
        })

        val client = Mqtt5Client.builder()
            .serverHost(broker.host)
            .serverPort(broker.port)
            .simpleAuth()
            .username("user")
            .password("pass".toByteArray())
            .applySimpleAuth()
            .buildBlocking()

        testAuthentication(client, broker, Mqtt5ConnAckReasonCode.SUCCESS)
    }

    @Test(expected = Mqtt5ConnAckException::class)
    fun testSimpleAuthenticationFailure() {
        val broker = Broker(authentication = object : Authentication {
            override fun authenticate(username: String?, password: UByteArray?): Boolean {
                return username == "user" && password?.toByteArray()?.decodeToString() == "pass"
            }
        })

        val client = Mqtt5Client.builder()
            .serverHost(broker.host)
            .serverPort(broker.port)
            .simpleAuth()
            .username("user2")
            .password("pass".toByteArray())
            .applySimpleAuth()
            .buildBlocking()

        testAuthentication(client, broker, Mqtt5ConnAckReasonCode.NOT_AUTHORIZED)
    }

    class TestEnAuth(private val callback: (success: Boolean) -> Unit) : Mqtt5EnhancedAuthMechanism {
        override fun getTimeout(): Int = 100

        override fun onContinue(
            clientConfig: Mqtt5ClientConfig,
            auth: Mqtt5Auth,
            authBuilder: Mqtt5AuthBuilder
        ): CompletableFuture<Boolean> {
            return CompletableFuture.completedFuture(true)
        }

        override fun getMethod(): MqttUtf8String = MqttUtf8String.of("TEST-EN-AUTH")

        override fun onAuth(
            clientConfig: Mqtt5ClientConfig,
            connect: Mqtt5Connect,
            authBuilder: Mqtt5EnhancedAuthBuilder
        ): CompletableFuture<Void> {
            val future = CompletableFuture<Void>()
            future.complete(null)
            return future
        }

        override fun onReAuthRejected(clientConfig: Mqtt5ClientConfig, disconnect: Mqtt5Disconnect) {
            callback(false)
        }

        override fun onAuthRejected(clientConfig: Mqtt5ClientConfig, connAck: Mqtt5ConnAck) {
            callback(false)
        }

        override fun onReAuthSuccess(clientConfig: Mqtt5ClientConfig, auth: Mqtt5Auth): CompletableFuture<Boolean> {
            return CompletableFuture.completedFuture(true)
        }

        override fun onReAuth(clientConfig: Mqtt5ClientConfig, authBuilder: Mqtt5AuthBuilder): CompletableFuture<Void> {
            return CompletableFuture.completedFuture(null)
        }

        override fun onAuthSuccess(clientConfig: Mqtt5ClientConfig, connAck: Mqtt5ConnAck): CompletableFuture<Boolean> =
            CompletableFuture.completedFuture(true)

        override fun onAuthError(clientConfig: Mqtt5ClientConfig, cause: Throwable) = throw cause

        override fun onReAuthError(clientConfig: Mqtt5ClientConfig, cause: Throwable) = throw cause
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
                    } else
                        result(EnhancedAuthenticationProvider.Result.SUCCESS, null)
                }
            }))

        val client = Mqtt5Client.builder()
            .serverHost(broker.host)
            .serverPort(broker.port)
            .enhancedAuth(TestEnAuth {
                assertTrue { it }
            })
            .buildBlocking()

        testAuthentication(client, broker, Mqtt5ConnAckReasonCode.SUCCESS)
    }
}
