import java.net.Socket


class ClientHandler(private val client: Socket) {
    private val reader = client.getInputStream()
    private val writer = client.getOutputStream()
    private var running = false

    fun run() {
        running = true

        while (running) {
            try {
                // TODO handle messages
            } catch (ex: Exception) {
                // TODO: Implement exception handling
                close()
            }
        }
    }

    private fun close() {
        running = false
        client.close()
    }
}
