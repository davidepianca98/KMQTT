package socket

import mqtt.broker.cluster.ClusterConnection

interface ServerSocketInterface {

    fun addClusterConnection(address: String): ClusterConnection?
}
