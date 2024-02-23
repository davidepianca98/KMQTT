import node.fs.ReadFileSyncBufferOptions
import socket.IOException
import socket.tls.ConnectionOptions
import socket.tls.TLSClientSettings
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
    if (tlsSettings.serverCertificate != null) {
        ca = if (tlsSettings.serverCertificate!!.isValidPem()) {
            tlsSettings.serverCertificate?.encodeToByteArray()?.toUByteArray()?.toBuffer()
        } else {
            // Try to load file
            tlsSettings.serverCertificate?.run { node.fs.readFileSync(this, ReadFileOptions()) } ?: throw IOException("Couldn't load server certificate")
        }
    }
    if (tlsSettings.clientCertificate != null) {
        cert = if (tlsSettings.clientCertificate!!.isValidPem()) {
            tlsSettings.clientCertificate?.encodeToByteArray()?.toUByteArray()?.toBuffer()
        } else {
            // Try to load file
            tlsSettings.clientCertificate?.run { node.fs.readFileSync(this, ReadFileOptions()) } ?: throw IOException("Couldn't load client certificate")
        }
    }
    if (tlsSettings.clientCertificateKey != null) {
        key = if (tlsSettings.clientCertificateKey!!.isValidPem()) {
            tlsSettings.clientCertificateKey?.encodeToByteArray()?.toUByteArray()?.toBuffer()
        } else {
            // Try to load file
            tlsSettings.clientCertificateKey?.run { node.fs.readFileSync(this, ReadFileOptions()) } ?: throw IOException("Couldn't load client certificate key")
        }
    }

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