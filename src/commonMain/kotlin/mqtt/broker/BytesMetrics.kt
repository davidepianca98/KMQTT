package mqtt.broker

interface BytesMetrics {

    fun received(clientId: String, bytesCount: Long)

    fun sent(clientId: String, bytesCount: Long)
}
