package socket.tls

import mqtt.broker.Broker
import socket.ServerSocket

actual class TLSServerSocket actual constructor(broker: Broker) : ServerSocket(broker) // TODO
