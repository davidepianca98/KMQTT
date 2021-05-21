package socket.tls

import cnames.structs.stack_st_X509
import kotlinx.cinterop.*
import mqtt.broker.Broker
import openssl.*
import platform.posix.fclose
import platform.posix.fopen
import socket.tcp.IOException

actual class TLSEngine actual constructor(serverContext: TLSServerContext) {

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

        val clientContext = SSL_new(serverContext.sslContext)
        if (clientContext == null) {
            BIO_free(readBio)
            BIO_free(writeBio)
            throw IOException("Failed allocating read BIO")
        }

        SSL_set_accept_state(clientContext)
        SSL_set_bio(clientContext, readBio, writeBio)
        context = clientContext
        this.readBio = readBio
        this.writeBio = writeBio
    }

    actual val isInitFinished: Boolean
        get() = SSL_is_init_finished(context) != 0

    actual val bioShouldRetry: Boolean
        get() = BIO_test_flags(writeBio, BIO_FLAGS_SHOULD_RETRY) == 0

    actual fun accept(): Int {
        return SSL_accept(context)
    }

    actual fun write(buffer: CPointer<ByteVar>, length: Int): Int {
        return SSL_write(context, buffer, length)
    }

    actual fun read(buffer: CPointer<ByteVar>, length: Int): Int {
        return SSL_read(context, buffer, length)
    }

    actual fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int {
        return BIO_read(writeBio, buffer, length)
    }

    actual fun getError(result: Int): TLSError {
        return when (SSL_get_error(context, result)) {
            SSL_ERROR_NONE -> TLSError.OK
            SSL_ERROR_WANT_READ -> TLSError.WANT_READ
            SSL_ERROR_ZERO_RETURN, SSL_ERROR_SYSCALL -> TLSError.ERROR
            else -> TLSError.ERROR
        }
    }

    actual fun close() {
        SSL_free(context)
    }
}

actual class TLSServerContext actual constructor(private val broker: Broker) {

    val sslContext: CPointer<SSL_CTX>

    init {
        OPENSSL_init_ssl(0u, null)
        val method = TLS_server_method()
        sslContext = SSL_CTX_new(method)!!

        if (broker.tlsSettings?.requireClientCertificate == true) {
            SSL_CTX_set_verify(sslContext, SSL_VERIFY_PEER or SSL_VERIFY_FAIL_IF_NO_PEER_CERT, null)
        }

        val pkcs12File =
            fopen(broker.tlsSettings!!.keyStoreFilePath, "rb") ?: throw Exception("PKCS12 keystore not found")
        val p12Cert = d2i_PKCS12_fp(pkcs12File, null)
        fclose(pkcs12File)

        memScoped {
            val privateKey = alloc<CPointerVar<EVP_PKEY>>()
            val x509Cert = alloc<CPointerVar<X509>>()
            val additionalCerts = alloc<CPointerVar<stack_st_X509>>()
            if (PKCS12_parse(
                    p12Cert,
                    broker.tlsSettings.keyStorePassword,
                    privateKey.ptr,
                    x509Cert.ptr,
                    additionalCerts.ptr
                ) != 1
            ) {
                throw Exception("Error parsing PKCS12 keystore")
            }

            SSL_CTX_use_certificate(sslContext, x509Cert.value)
            SSL_CTX_use_PrivateKey(sslContext, privateKey.value)
            if (SSL_CTX_check_private_key(sslContext) != 1) {
                throw Exception("Error checking private key match with the public certificate")
            }
        }
    }

    actual fun close() {
        SSL_CTX_free(sslContext)
    }
}
