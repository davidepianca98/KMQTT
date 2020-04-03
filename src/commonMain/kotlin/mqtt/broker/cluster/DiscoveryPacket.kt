package mqtt.broker.cluster

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

@Serializable
data class DiscoveryPacket(
    @ProtoId(1)
    val name: String = ""
)
