package socket.tls

import mqtt.Broker
import socket.ServerSocket

expect class TLSServerSocket(broker: Broker) : ServerSocket
