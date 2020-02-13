package mqtt.broker

interface BytesMetrics {

    fun connectionClosed(clientId: String, bytesSent: Long, bytesReceived: Long)
}
