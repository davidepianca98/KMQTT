/**
 * TLS settings
 *
 * @param version the TLS version
 * @param serverCertificatePath if the server certificate cannot be verified with the default chain, set this to the path of the server certificate file in PEM format, null otherwise
 * @param clientCertificateKeyStorePath the path to the client certificate PKCS12 key store if client verification necessary, null otherwise
 * @param clientCertificateKeyStorePassword the password to the client certificate PKCS12 key store if client verification necessary, null otherwise
 */
data class TLSClientSettings(
    val version: String = "TLS",
    val serverCertificatePath: String? = null,
    val clientCertificateKeyStorePath: String? = null,
    val clientCertificateKeyStorePassword: String? = null
)
