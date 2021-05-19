actual fun setShutdownHook(hook: () -> Unit) {
    process.on("beforeExit") { _ ->
        hook()
    }
}
