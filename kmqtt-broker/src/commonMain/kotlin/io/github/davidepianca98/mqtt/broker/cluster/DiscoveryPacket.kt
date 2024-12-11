package io.github.davidepianca98.mqtt.broker.cluster

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class DiscoveryPacket(
    @ProtoNumber(1)
    val name: String = ""
)
