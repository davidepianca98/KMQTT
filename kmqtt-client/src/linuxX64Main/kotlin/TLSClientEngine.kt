import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.toKString
import openssl.*
import platform.posix.getenv
import socket.IOException
import socket.tls.TLSEngine

actual class TLSClientEngine actual constructor(tlsSettings: TLSClientSettings) : TLSEngine {

    private val context: CPointer<SSL>
    private val readBio: CPointer<BIO>
    private val writeBio: CPointer<BIO>

    init {
        val readBio = BIO_new(BIO_s_mem()) ?: throw IOException("Failed allocating read BIO")

        val writeBio = BIO_new(BIO_s_mem())
        if (writeBio == null) {
            BIO_free(readBio)
            throw IOException("Failed allocating read BIO")
        }

        val method = TLS_client_method()
        val sslContext = SSL_CTX_new(method)!!

        val clientContext = SSL_new(sslContext)
        if (clientContext == null) {
            BIO_free(readBio)
            BIO_free(writeBio)
            throw IOException("Failed allocating read BIO")
        }

        SSL_set_verify(clientContext, SSL_VERIFY_PEER, null)

        if (tlsSettings.serverCertificatePath != null) {
            if (SSL_CTX_load_verify_locations(sslContext, tlsSettings.serverCertificatePath, null) != 1) {
                throw Exception("Server certificate path not found")
            }
        } else {
            if (SSL_CTX_load_verify_locations(sslContext, null, getenv(X509_get_default_cert_dir_env()?.toKString())?.toKString()) != 1) {
                throw Exception("Server certificate path not found")
            }
        }

        if (tlsSettings.clientCertificatePath != null) {
            if (SSL_CTX_use_certificate_file(sslContext, tlsSettings.clientCertificatePath, SSL_FILETYPE_PEM) != 1) {
                throw Exception("Cannot load client's certificate file")
            }
            if (SSL_CTX_use_PrivateKey_file(sslContext, tlsSettings.clientCertificateKeyPath!!, SSL_FILETYPE_PEM) != 1) {
                throw Exception("Cannot load client's key file")
            }
            if (SSL_CTX_check_private_key(sslContext) != 1) {
                throw Exception("Client's certificate and key don't match")
            }
        }

        SSL_set_connect_state(clientContext)
        SSL_set_bio(clientContext, readBio, writeBio)
        context = clientContext
        this.readBio = readBio
        this.writeBio = writeBio
    }

    override val isInitFinished: Boolean
        get() = SSL_is_init_finished(context) != 0

    override val bioShouldRetry: Boolean
        get() = BIO_test_flags(writeBio, BIO_FLAGS_SHOULD_RETRY) == 0

    override fun write(buffer: CPointer<ByteVar>, length: Int): Int {
        return SSL_write(context, buffer, length)
    }

    override fun read(buffer: CPointer<ByteVar>, length: Int): Int {
        return SSL_read(context, buffer, length)
    }

    override fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int {
        return BIO_read(writeBio, buffer, length)
    }

    override fun bioWrite(buffer: CPointer<ByteVar>, length: Int): Int {
        return BIO_write(readBio, buffer, length)
    }

    override fun getError(result: Int): Int {
        return SSL_get_error(context, result)
    }

    override fun close() {
        SSL_free(context)
    }
}