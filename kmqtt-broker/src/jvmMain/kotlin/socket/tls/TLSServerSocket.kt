package socket.tls

import mqtt.broker.Broker
import socket.ServerSocket
import socket.SocketState
import socket.tcp.Socket
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext


actual class TLSServerSocket actual constructor(
    private val broker: Broker,
    selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : ServerSocket(broker, selectCallback) {

    private val sslContext = SSLContext.getInstance(broker.tlsSettings!!.version)

    init {
        val keyStore = KeyStore.getInstance("PKCS12")
        File(broker.tlsSettings!!.keyStoreFilePath).inputStream().use {
            keyStore.load(it, broker.tlsSettings.keyStorePassword?.toCharArray())
        }
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, broker.tlsSettings.keyStorePassword?.toCharArray())

        sslContext.init(keyManagerFactory.keyManagers, null, null)

        val initSession = sslContext.createSSLEngine().session
        initSession.invalidate()
    }

    private fun buildKeyManagers(
        certificateBytes: ByteArray,
        keyAlgorithm: String,
        privateKeyBytes: ByteArray,
        publicKeyBytes: ByteArray,
        privateKeyPassword: String?
    ): Array<KeyManager> {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificate = certificateFactory.generateCertificate(ByteArrayInputStream(certificateBytes))

        val keyFactory = KeyFactory.getInstance(keyAlgorithm)
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes))
        certificate.verify(publicKey)

        keyStore.setCertificateEntry("main", certificate)
        keyStore.setKeyEntry("main", privateKey, privateKeyPassword?.toCharArray(), arrayOf(certificate))

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, null)
        return kmf.keyManagers
    }

    override fun createSocket(channel: SocketChannel, socketKey: SelectionKey): Socket {
        val engine = sslContext.createSSLEngine()
        engine.useClientMode = false
        if (broker.tlsSettings?.requireClientCertificate == true) {
            engine.needClientAuth = true
        }
        val sendBuffer = ByteBuffer.allocate(engine.session.packetBufferSize)
        val receiveBuffer = ByteBuffer.allocate(engine.session.packetBufferSize)
        val sendAppBuffer = ByteBuffer.allocate(engine.session.applicationBufferSize)
        val receiveAppBuffer = ByteBuffer.allocate(engine.session.applicationBufferSize)
        return TLSSocket(channel, socketKey, sendBuffer, receiveBuffer, sendAppBuffer, receiveAppBuffer, engine)
    }
}
