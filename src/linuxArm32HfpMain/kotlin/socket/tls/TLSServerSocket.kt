package socket.tls

import mqtt.Broker
import socket.ServerSocket

actual class TLSServerSocket actual constructor(broker: Broker) : ServerSocket(broker) // TODO
