package io.github.davidepianca98

import io.github.davidepianca98.socket.tls.TLSClientSettings
import io.github.davidepianca98.socket.tls.TLSSocket
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.RSAPrivateKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngineResult
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


public actual class TLSClientSocket actual constructor(
    address: String,
    port: Int,
    maximumPacketSize: Int,
    private val readTimeOut: Int,
    private val connectTimeOut: Int,
    private val tlsSettings: TLSClientSettings,
    checkCallback: () -> Unit
) : TLSSocket(
    SocketChannel.open().apply {
        socket().connect(InetSocketAddress(address, port), connectTimeOut)
        configureBlocking(false)
    },
    null,
    ByteBuffer.allocate(maximumPacketSize),
    ByteBuffer.allocate(maximumPacketSize),
    ByteBuffer.allocate(maximumPacketSize),
    ByteBuffer.allocate(maximumPacketSize),
    SSLContext.getInstance(tlsSettings.version).apply {
        val trustManagers = if (tlsSettings.serverCertificate != null) {
            val certificate = CertificateFactory
                .getInstance("X.509")
                .generateCertificate(if (tlsSettings.serverCertificate!!.isValidPem()) tlsSettings.serverCertificate?.byteInputStream() else FileInputStream(tlsSettings.serverCertificate!!))

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry("server", certificate)

            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)

            trustManagerFactory.trustManagers
        } else {
            if (!tlsSettings.checkServerCertificate) {
                arrayOf(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                })
            } else {
                null
            }
        }

        val keyManagers = if (tlsSettings.clientCertificate != null) {
            val certificate = CertificateFactory
                .getInstance("X.509")
                .generateCertificate(if (tlsSettings.clientCertificate!!.isValidPem()) tlsSettings.clientCertificate?.byteInputStream() else FileInputStream(tlsSettings.clientCertificate!!))

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            val keyContent = if (tlsSettings.clientCertificateKey!!.isValidPem()) tlsSettings.clientCertificateKey!! else FileInputStream(tlsSettings.clientCertificateKey!!).bufferedReader().readText();
            val key = try {
                getRSAPrivateKeyFromString(keyContent)
            } catch (e: InvalidKeySpecException) {
                getECPrivateKeyFromString(keyContent)
            }
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

        val params = sslParameters
        tlsSettings.alpnProtocols?.let {
            params.applicationProtocols = it.toTypedArray()
        }
        tlsSettings.serverNameIndications?.let {
            params.serverNames = listOf(SNIHostName(it))
        }
        sslParameters = params
    }
) {
    private val selector = Selector.open()

    private val sendPacketQueue = mutableListOf<UByteArray>()

    init {
        channel.register(selector, SelectionKey.OP_READ)
    }

    public companion object {
        private fun getRSAPrivateKeyFromString(key: String): RSAPrivateKey {
            val privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n","")
            val encoded = Base64.getDecoder().decode(privateKeyPEM)
            val kf = KeyFactory.getInstance("RSA")
            val keySpec = PKCS8EncodedKeySpec(encoded)
            return kf.generatePrivate(keySpec) as RSAPrivateKey
        }

        private fun getECPrivateKeyFromString(key: String): ECPrivateKey {
            val privateKeyPEM = key
                .replace("-----BEGIN EC PRIVATE KEY-----", "")
                .replace("-----END EC PRIVATE KEY-----", "")
                .replace("\n","")
            val encoded = Base64.getDecoder().decode(privateKeyPEM)
            val kf = KeyFactory.getInstance("EC")
            val keySpec = PKCS8EncodedKeySpec(encoded)
            return kf.generatePrivate(keySpec) as ECPrivateKey
        }
    }

    override fun send(data: UByteArray) {
        if (!handshakeComplete) {
            sendPacketQueue.add(data)
        } else {
            super.send(data)
        }
    }

    override fun read(): UByteArray? {
        if (handshakeComplete) {
            val iterator = sendPacketQueue.iterator()
            while (iterator.hasNext()) {
                send(iterator.next())
                iterator.remove()
            }
        }

        val count = selector.select(readTimeOut.toLong())
        return if (count > 0) {
            selector.selectedKeys().clear()
            super.read()
        } else {
            null
        }
    }

    public actual val handshakeComplete: Boolean
        get() = engine.handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED
                || engine.handshakeStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
}
