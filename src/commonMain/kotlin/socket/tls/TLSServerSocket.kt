package socket.tls

import mqtt.broker.Broker
import socket.ServerSocket
import socket.ServerSocketLoop

expect class TLSServerSocket(
    broker: Broker,
    selectCallback: (attachment: Any?, state: ServerSocketLoop.SocketState) -> Boolean
) : ServerSocket
