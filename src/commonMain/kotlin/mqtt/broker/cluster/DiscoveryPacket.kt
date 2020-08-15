package mqtt.broker.cluster

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class DiscoveryPacket(
    @ProtoNumber(1)
    val name: String = ""
)
