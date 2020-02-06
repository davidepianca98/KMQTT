package socket.tls

import mqtt.broker.Broker
import socket.ServerSocket

expect class TLSServerSocket(broker: Broker) : ServerSocket
