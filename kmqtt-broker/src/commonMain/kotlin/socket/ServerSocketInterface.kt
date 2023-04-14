package socket

import mqtt.broker.cluster.ClusterConnection

internal interface ServerSocketInterface {

    fun addClusterConnection(address: String): ClusterConnection?
}
