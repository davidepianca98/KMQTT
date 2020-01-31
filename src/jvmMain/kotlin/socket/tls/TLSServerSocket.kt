package socket.tls

import mqtt.Broker
import mqtt.ClientConnection
import socket.ServerSocket
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


actual class TLSServerSocket actual constructor(private val broker: Broker) : ServerSocket(broker) {

    private val sslContext = SSLContext.getInstance(broker.tlsSettings!!.version)
    private var sendAppBuffer = ByteBuffer.allocate(0)
    private var receiveAppBuffer = ByteBuffer.allocate(0)

    init { // TODO generate test localhost certificate and put it in tlsSettings
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificate = certificateFactory.generateCertificate(ByteArrayInputStream(broker.tlsSettings!!.certificate))
        keyStore.setCertificateEntry("main", certificate)

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, null)

        sslContext.init(kmf.keyManagers, arrayOf(object : X509TrustManager { // TODO implement correct trust manager
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {

            }

            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {

            }

            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                return null
            }

        }), null)

        val initSession = sslContext.createSSLEngine().session
        sendBuffer = ByteBuffer.allocate(initSession.packetBufferSize)
        receiveBuffer = ByteBuffer.allocate(initSession.packetBufferSize)
        sendAppBuffer = ByteBuffer.allocate(initSession.applicationBufferSize + 50)
        receiveAppBuffer = ByteBuffer.allocate(initSession.applicationBufferSize + 50)
        initSession.invalidate()
    }

    override fun accept(key: SelectionKey) {
        try {
            val channel = (key.channel() as ServerSocketChannel).accept()
            channel.configureBlocking(false)

            val engine = sslContext.createSSLEngine()
            val socket = TLSSocket(sendBuffer, receiveBuffer, sendAppBuffer, receiveAppBuffer, engine)

            val clientConnection = ClientConnection(socket, broker)

            val socketKey = channel.register(selector, SelectionKey.OP_READ, clientConnection)
            socket.key = socketKey
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
    }
}
