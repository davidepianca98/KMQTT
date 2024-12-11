package io.github.davidepianca98.socket

import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.broker.cluster.ClusterConnection

internal expect open class ServerSocket(
    broker: Broker,
    selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : ServerSocketInterface {

    fun isRunning(): Boolean

    fun select(timeout: Long)

    fun close()

    override fun addClusterConnection(address: String): ClusterConnection?
}
