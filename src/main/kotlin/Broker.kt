import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketAddress
import kotlin.concurrent.thread

// TODO 3.3
@ExperimentalUnsignedTypes
class Broker(
    local: SocketAddress,
    backlog: Int = 128,
    val maximumSessionExpiryInterval: UInt = 0xFFFFFFFFu,
    val receiveMaximum: Int? = null,
    val maximumQos: Int? = null,
    val retainedAvailable: Boolean = true,
    val maximumPacketSize: UInt? = null,
    val maximumTopicAlias: Int? = null,
    val wildcardSubscriptionAvailable: Boolean = true,
    val subscriptionIdentifiersAvailable: Boolean = true,
    val sharedSubscriptionsAvailable: Boolean = true,
    val serverKeepAlive: Int? = null
) {

    constructor(port: Int, host: String = "127.0.0.1") : this(InetSocketAddress(host, port))

    private val server = ServerSocket()

    init {
        receiveMaximum?.let {
            require(it in 0..65535)
        }
        maximumQos?.let {
            require(it in 0..2)
        }

        server.bind(local, backlog)
    }

    fun listen() {
        while (true) {
            val client = server.accept()
            thread { ClientHandler(client, null, this).run() }
        }
    }
}
