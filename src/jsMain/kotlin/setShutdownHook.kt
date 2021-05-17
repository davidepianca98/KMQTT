actual fun setShutdownHook(hook: () -> Unit) {
    process.on("beforeExit") { code ->
        hook()
    }
}
