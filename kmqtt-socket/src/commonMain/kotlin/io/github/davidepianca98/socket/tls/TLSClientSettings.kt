package io.github.davidepianca98.socket.tls

/**
 * TLS settings
 *
 * @param version the TLS version
 * @param serverCertificate if the server certificate cannot be verified with the default chain, set this to the path
 *                          of the server certificate file in PEM format, null otherwise
 * @param clientCertificate the PEM client certificate string or the path to the client certificate file in PEM format
 *                          if client verification is necessary, null otherwise
 * @param clientCertificateKey the PEM client certificate key string or the path to the client certificate key file in
 *                          PEM format if client verification necessary, null otherwise
 * @param clientCertificatePassword the password to the client certificate key if client verification necessary and the
 *                          key has a password, null otherwise
 */
public data class TLSClientSettings(
    val version: String = "TLS",
    val serverCertificate: String? = null,
    val clientCertificate: String? = null,
    val clientCertificateKey: String? = null,
    val clientCertificatePassword: String? = null,
    val checkServerCertificate: Boolean = true
)
