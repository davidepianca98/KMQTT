import node.events.EventType
import node.process.process

internal actual fun setShutdownHook(hook: () -> Unit) {
    process.on(EventType("BEFORE_EXIT")) {
        hook()
    }
}
