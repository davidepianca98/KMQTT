import socket.tls.TLSSocket

private fun TlsConnectionOptions(): tls.ConnectionOptions = js("{}") as tls.ConnectionOptions

actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int,
    tlsSettings: TLSClientSettings,
    private val checkCallback: () -> Unit
) : TLSSocket(tls.connect(port, address, TlsConnectionOptions().apply {
    ca = tlsSettings.serverCertificatePath?.run { fs.readFileSync(this, null as String?) } ?: ca
    cert = tlsSettings.clientCertificatePath?.run { fs.readFileSync(this, null as String?) } ?: cert
    key = tlsSettings.clientCertificateKeyPath?.run { fs.readFileSync(this, null as String?) } ?: key
    passphrase = tlsSettings.clientCertificatePassword ?: passphrase
    servername = address
}), { _, _ ->
    checkCallback()
    true
})
{
    actual val handshakeComplete: Boolean
        get() = true

    init {
        doLater()
    }

    private fun doLater() {
        setTimeout({
            checkCallback()
            doLater()
        }, 250)
    }
}