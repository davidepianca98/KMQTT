package mqtt.broker.cluster

import kotlin.random.Random

data class ClusterSettings(
    val name: String = Random.nextLong().toString(),
    val tcpPort: Int = 22222,
    val discoveryPort: Int = 22223
)
