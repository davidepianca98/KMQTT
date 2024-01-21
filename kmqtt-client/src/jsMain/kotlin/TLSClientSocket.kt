import node.fs.ReadFileSyncBufferOptions
import socket.tls.ConnectionOptions
import socket.tls.TLSSocket
import socket.tls.connect
import web.timers.setTimeout

private fun TlsConnectionOptions(): ConnectionOptions = js("{ checkServerIdentity: function (host, cert) { return undefined; }}") as ConnectionOptions

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
    try {
        checkCallback()
        true
    } catch (e: dynamic) {
        false
    }
})
{
    public actual val handshakeComplete: Boolean
        get() = true

    private var open = true

    init {
        doLater()
    }

    private fun doLater() {
        if (open) {
            setTimeout({
                try {
                    checkCallback()
                    doLater()
                } catch (e: dynamic) {
                    close()
                }
            }, 250)
        }
    }

    override fun close() {
        open = false
        super.close()
    }
}