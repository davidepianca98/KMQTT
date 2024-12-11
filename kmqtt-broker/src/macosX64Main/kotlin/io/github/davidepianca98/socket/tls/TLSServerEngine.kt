package io.github.davidepianca98.socket.tls

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import openssl.*
import io.github.davidepianca98.socket.IOException

internal actual class TLSServerEngine actual constructor(serverContext: TLSServerContext) : TLSEngine {

    private val context: CPointer<SSL>
    private val readBio: CPointer<BIO>
    private val writeBio: CPointer<BIO>

    private var freed = false

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

    actual override val isInitFinished: Boolean
        get() = SSL_is_init_finished(context) != 0

    actual override val bioShouldRetry: Boolean
        get() = BIO_test_flags(writeBio, BIO_FLAGS_SHOULD_RETRY) == 0

    actual override fun write(buffer: CPointer<ByteVar>, length: Int): Int {
        return SSL_write(context, buffer, length)
    }

    actual override fun read(buffer: CPointer<ByteVar>, length: Int): Int {
        return SSL_read(context, buffer, length)
    }

    actual override fun bioRead(buffer: CPointer<ByteVar>, length: Int): Int {
        return BIO_read(writeBio, buffer, length)
    }

    actual override fun bioWrite(buffer: CPointer<ByteVar>, length: Int): Int {
        return BIO_write(readBio, buffer, length)
    }

    actual override fun getError(result: Int): Int {
        return SSL_get_error(context, result)
    }

    actual override fun close() {
        if (!freed) {
            SSL_free(context)
            freed = true
        }
    }
}
