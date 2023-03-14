package socket.tls

expect class TLSServerContext(tlsSettings: TLSSettings) {
    fun close()
}