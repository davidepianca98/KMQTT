package socket.tls

class TLSSettings(
    val version: String = "TLS",
    val keyStoreFilePath: String,
    val keyStorePassword: String? = null
)
