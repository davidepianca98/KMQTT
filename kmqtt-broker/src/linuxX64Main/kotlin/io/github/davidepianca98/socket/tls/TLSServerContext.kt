package io.github.davidepianca98.socket.tls

import cnames.structs.stack_st_X509
import kotlinx.cinterop.*
import openssl.*
import platform.posix.fclose
import platform.posix.fopen


internal actual class TLSServerContext actual constructor(private val tlsSettings: TLSSettings) {

    val sslContext: CPointer<SSL_CTX>

    init {
        OPENSSL_init_ssl(0u, null)
        val method = TLS_server_method()
        sslContext = SSL_CTX_new(method)!!

        if (tlsSettings.requireClientCertificate) {
            SSL_CTX_set_verify(sslContext, SSL_VERIFY_PEER or SSL_VERIFY_FAIL_IF_NO_PEER_CERT, null)
        } else {
            SSL_CTX_set_verify(sslContext, SSL_VERIFY_PEER, null)
        }

        val pkcs12File =
            fopen(tlsSettings.keyStoreFilePath, "rb") ?: throw Exception("PKCS12 keystore not found")
        val p12Cert = d2i_PKCS12_fp(pkcs12File, null)
        fclose(pkcs12File)

        memScoped {
            val privateKey = alloc<CPointerVar<EVP_PKEY>>()
            val x509Cert = alloc<CPointerVar<X509>>()
            val additionalCerts = alloc<CPointerVar<stack_st_X509>>()
            if (PKCS12_parse(
                    p12Cert,
                    tlsSettings.keyStorePassword,
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
