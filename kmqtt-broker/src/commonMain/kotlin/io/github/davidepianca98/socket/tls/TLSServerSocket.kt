package io.github.davidepianca98.socket.tls

import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.socket.ServerSocket
import io.github.davidepianca98.socket.SocketState

internal expect class TLSServerSocket(
    broker: Broker,
    selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : ServerSocket
