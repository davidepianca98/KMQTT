import node.events.Event
import node.process.process

internal actual fun setShutdownHook(hook: () -> Unit) {
    process.on(Event.BEFORE_EXIT) { _ ->
        hook()
    }
}
