package mqtt.broker.cluster

import mqtt.Subscription
import mqtt.broker.Session

fun MutableMap<String, ClusterConnection>.setRetained(retained: Pair<mqtt.packets.mqtt.MQTTPublish, String>) {
    forEach {
        it.value.setRetained(retained)
    }
}

fun MutableMap<String, ClusterConnection>.addSubscription(clientId: String, subscription: Subscription) {
    forEach {
        it.value.addSubscription(clientId, subscription)
    }
}

fun MutableMap<String, ClusterConnection>.removeSubscription(clientId: String, topicFilter: String) {
    forEach {
        it.value.removeSubscription(clientId, topicFilter)
    }
}

fun MutableMap<String, ClusterConnection>.addSession(session: Session) {
    forEach {
        it.value.addSession(session)
    }
}

fun MutableMap<String, ClusterConnection>.updateSession(session: Session) {
    forEach {
        it.value.updateSession(session)
    }
}
