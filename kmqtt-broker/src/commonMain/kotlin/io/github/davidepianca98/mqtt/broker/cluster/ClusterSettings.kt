package io.github.davidepianca98.mqtt.broker.cluster

import kotlin.random.Random

public data class ClusterSettings(
    val name: String = Random.nextLong().toString(),
    val tcpPort: Int = 22222,
    val dnsDiscovery: Boolean = true,
    val dnsName: String = "kmqtt_broker",
    val discoveryPort: Int = 22223
)
