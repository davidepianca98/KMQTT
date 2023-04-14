package socket.tls

internal expect class TLSServerContext(tlsSettings: TLSSettings) {
    fun close()
}