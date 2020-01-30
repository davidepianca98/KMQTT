import mqtt.Broker

actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

fun main() {
    Broker().listen()
}
