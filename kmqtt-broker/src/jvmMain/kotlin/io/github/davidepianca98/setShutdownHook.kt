package io.github.davidepianca98

public actual fun setShutdownHook(hook: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            hook()
        }
    })
}
