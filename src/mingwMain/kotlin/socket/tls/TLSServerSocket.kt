package socket.tls

import mqtt.Broker
import socket.ServerSocket

actual class TLSServerSocket actual constructor(private val broker: Broker) : ServerSocket(broker)
