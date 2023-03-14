package socket.udp

data class UDPReadData(val data: UByteArray, val sourceAddress: String, val sourcePort: Int)
