/**
 * TLS settings
 *
 * @param version the TLS version
 * @param serverCertificatePath if the server certificate cannot be verified with the default chain, set this to the path of the server certificate file in PEM format, null otherwise
 * @param clientCertificatePath the path to the client certificate file in PEM format if client verification necessary, null otherwise
 * @param clientCertificateKeyPath the path to the client certificate key file in PEM format if client verification necessary, null otherwise
 * @param clientCertificatePassword the password to the client certificate key if client verification necessary and the key has a password, null otherwise
 */
public data class TLSClientSettings(
    val version: String = "TLS",
    val serverCertificatePath: String? = null,
    val clientCertificatePath: String? = null,
    val clientCertificateKeyPath: String? = null,
    val clientCertificatePassword: String? = null
)
