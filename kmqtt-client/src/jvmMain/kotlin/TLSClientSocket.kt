import socket.tls.TLSSocket
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory


actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    private val readTimeOut: Int,
    private val tlsSettings: TLSClientSettings
) : TLSSocket(
    SocketChannel.open(InetSocketAddress(address, port)).apply {
        configureBlocking(false)
    },
    null,
    ByteBuffer.allocate(maximumPacketSize),
    ByteBuffer.allocate(maximumPacketSize),
    ByteBuffer.allocate(maximumPacketSize),
    ByteBuffer.allocate(maximumPacketSize),
    SSLContext.getInstance(tlsSettings.version).apply {
        val trustManagers = if (tlsSettings.serverCertificatePath != null) {
            val certificate = CertificateFactory
                .getInstance("X.509")
                .generateCertificate(FileInputStream(tlsSettings.serverCertificatePath!!))

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry("server", certificate)

            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)

            trustManagerFactory.trustManagers
        } else {
            null
        }

        val keyManagers = if (tlsSettings.clientCertificatePath != null) {
            val certificate = CertificateFactory
                .getInstance("X.509")
                .generateCertificate(FileInputStream(tlsSettings.clientCertificatePath!!))

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            val key = getPrivateKeyFromString(FileInputStream(tlsSettings.clientCertificateKeyPath!!).bufferedReader().readText())
            keyStore.setKeyEntry("client", key, tlsSettings.clientCertificatePassword?.toCharArray(), arrayOf(certificate))

            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(keyStore, tlsSettings.clientCertificatePassword?.toCharArray())
            kmf.keyManagers
        } else {
            null
        }

        init(keyManagers, trustManagers, null)
    }.createSSLEngine().apply {
        useClientMode = true
    }
) {
    private val selector = Selector.open()

    init {
        channel.register(selector, SelectionKey.OP_READ)
    }

    companion object {
        private fun getPrivateKeyFromString(key: String): RSAPrivateKey {
            val privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----\n", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n","")
            val encoded = Base64.getDecoder().decode(privateKeyPEM)
            val kf = KeyFactory.getInstance("RSA")
            val keySpec = PKCS8EncodedKeySpec(encoded)
            return kf.generatePrivate(keySpec) as RSAPrivateKey
        }
    }

    override fun read(): UByteArray? {
        val count = selector.select(readTimeOut.toLong())
        return if (count > 0) {
            selector.selectedKeys().clear()
            super.read()
        } else {
            null
        }
    }
}
