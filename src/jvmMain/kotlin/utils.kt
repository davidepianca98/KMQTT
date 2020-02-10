import mqtt.broker.Broker
import socket.tls.TLSSettings
import java.nio.ByteBuffer

actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

fun ByteBuffer.toUByteArray(): UByteArray {
    val length = remaining()
    val array = ByteArray(length)
    get(array, 0, length)
    return array.toUByteArray()
}

fun main() {
    Broker(
        tlsSettings = TLSSettings(keyStoreFilePath = "keyStore.p12", keyStorePassword = "changeit"),
        port = 8883
    ).listen()
}
