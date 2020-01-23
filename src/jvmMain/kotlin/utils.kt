import java.util.*

actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

actual fun generateRandomClientId(): String {
    return UUID.randomUUID().toString()
}
