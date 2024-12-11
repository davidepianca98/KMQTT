package io.github.davidepianca98

import node.fs.ReadFileSyncBufferOptions
import io.github.davidepianca98.socket.IOException
import io.github.davidepianca98.socket.tls.ConnectionOptions
import io.github.davidepianca98.socket.tls.TLSClientSettings
import io.github.davidepianca98.socket.tls.TLSSocket
import io.github.davidepianca98.socket.tls.connect
import node.fs.readFileSync
import web.timers.setTimeout

private fun TlsConnectionOptions(): ConnectionOptions = js("{ checkServerIdentity: function (host, cert) { return undefined; }}") as ConnectionOptions

public actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    private val readTimeOut: Int,
    connectTimeOut: Int,
    tlsSettings: TLSClientSettings,
    private val checkCallback: () -> Unit
) : TLSSocket(connect(port, address, TlsConnectionOptions().apply {
    fun ReadFileOptions(): ReadFileSyncBufferOptions = js("{}") as ReadFileSyncBufferOptions
    if (tlsSettings.serverCertificate != null) {
        ca = if (tlsSettings.serverCertificate!!.isValidPem()) {
            tlsSettings.serverCertificate?.encodeToByteArray()?.toUByteArray()?.toBuffer()
        } else {
            // Try to load file
            tlsSettings.serverCertificate?.run { readFileSync(this, ReadFileOptions()) } ?: throw IOException("Couldn't load server certificate")
        }
    }
    if (tlsSettings.clientCertificate != null) {
        cert = if (tlsSettings.clientCertificate!!.isValidPem()) {
            tlsSettings.clientCertificate?.encodeToByteArray()?.toUByteArray()?.toBuffer()
        } else {
            // Try to load file
            tlsSettings.clientCertificate?.run { readFileSync(this, ReadFileOptions()) } ?: throw IOException("Couldn't load client certificate")
        }
    }
    if (tlsSettings.clientCertificateKey != null) {
        key = if (tlsSettings.clientCertificateKey!!.isValidPem()) {
            tlsSettings.clientCertificateKey?.encodeToByteArray()?.toUByteArray()?.toBuffer()
        } else {
            // Try to load file
            tlsSettings.clientCertificateKey?.run { readFileSync(this, ReadFileOptions()) } ?: throw IOException("Couldn't load client certificate key")
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
        setTimeout({
            if (socket.connecting) {
                close()
                throw IOException("Socket connect timeout set failed")
            }
        }, connectTimeOut)
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
            }, readTimeOut)
        }
    }

    override fun close() {
        open = false
        super.close()
    }
}