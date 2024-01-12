import node.fs.ReadFileSyncBufferOptions
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
    fun ReadFileOptions(): ReadFileSyncBufferOptions = js("{}") as ReadFileSyncBufferOptions
    ca = tlsSettings.serverCertificatePath?.run { node.fs.readFileSync(this, ReadFileOptions()) }
    cert = tlsSettings.clientCertificatePath?.run { node.fs.readFileSync(this, ReadFileOptions()) }
    key = tlsSettings.clientCertificateKeyPath?.run { node.fs.readFileSync(this, ReadFileOptions()) }
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