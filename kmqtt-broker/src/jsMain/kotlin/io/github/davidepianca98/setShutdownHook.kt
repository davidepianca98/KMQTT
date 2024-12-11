package io.github.davidepianca98

import node.process.ProcessEvent
import node.process.process

internal actual fun setShutdownHook(hook: () -> Unit) {
    process.on(ProcessEvent.BEFOREEXIT) {
        hook()
    }
}
