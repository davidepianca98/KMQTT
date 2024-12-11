package io.github.davidepianca98.socket.tls

internal expect class TLSServerContext(tlsSettings: TLSSettings) {
    fun close()
}