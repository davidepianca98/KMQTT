import socket.tls.ConnectionOptions
import socket.tls.TLSSocket
import socket.tls.connect
import web.timers.setTimeout

private fun TlsConnectionOptions(): ConnectionOptions = js("{}") as ConnectionOptions

public actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    readTimeOut: Int,
    tlsSettings: TLSClientSettings,
    private val checkCallback: () -> Unit
) : TLSSocket(connect(port, address, TlsConnectionOptions().apply {
    ca = tlsSettings.serverCertificatePath?.run { node.fs.readFileSync(this, null) }
    cert = tlsSettings.clientCertificatePath?.run { node.fs.readFileSync(this, null) }
    key = tlsSettings.clientCertificateKeyPath?.run { node.fs.readFileSync(this, null) }
    passphrase = tlsSettings.clientCertificatePassword
    servername = address
}), { _, _ ->
    checkCallback()
    true
})
{
    public actual val handshakeComplete: Boolean
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