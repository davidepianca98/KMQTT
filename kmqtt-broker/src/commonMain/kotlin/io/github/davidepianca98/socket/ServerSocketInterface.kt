package io.github.davidepianca98.socket

import io.github.davidepianca98.mqtt.broker.cluster.ClusterConnection

internal interface ServerSocketInterface {

    fun addClusterConnection(address: String): ClusterConnection?
}
