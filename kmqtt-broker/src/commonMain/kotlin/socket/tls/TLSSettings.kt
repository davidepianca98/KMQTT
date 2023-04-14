package socket.tls

public class TLSSettings(
    public val version: String = "TLS",
    public val keyStoreFilePath: String,
    public val keyStorePassword: String? = null,
    public val requireClientCertificate: Boolean = false
)
