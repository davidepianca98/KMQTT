import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.setShutdownHook
import io.github.davidepianca98.socket.tls.TLSSettings

private fun showHelp() {
    println("At least one argument needed:")
    println("    -h x.x.x.x")
    println("    -p port")
    println("    --max-connections n")
    println("    --key-store path")
    println("    --key-store-psw password")
    println("    --wsp port")
}

public fun main(args: Array<String>) {
    val argumentsMap = HashMap<String, String>()
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-h" -> argumentsMap["host"] = args[++i]
            "-p" -> argumentsMap["port"] = args[++i]
            "--max-connections" -> argumentsMap["maxConn"] = args[++i]
            "--key-store" -> argumentsMap["keyStore"] = args[++i]
            "--key-store-psw" -> argumentsMap["keyStorePassword"] = args[++i]
            "--wsp" -> argumentsMap["wsPort"] = args[++i]
            "--help" -> {
                showHelp()
                return
            }
        }
        i++
    }

    println("Starting KMQTT")

    val host = argumentsMap["host"] ?: "127.0.0.1"
    val port = argumentsMap["port"]?.toInt() ?: 1883
    val backlog = argumentsMap["maxConn"]?.toInt() ?: 128
    val tlsSettings = argumentsMap["keyStore"]?.let {
        TLSSettings(keyStoreFilePath = it, keyStorePassword = argumentsMap["keyStorePassword"])
    }
    val wsPort = 1884//argumentsMap["wsPort"]?.toInt()

    val broker = Broker(
        port = port,
        host = host,
        backlog = backlog,
        tlsSettings = tlsSettings,
        webSocketPort = wsPort
    )

    setShutdownHook {
        broker.stop()
    }

    println("KMQTT Started")
    broker.listen()
    println("KMQTT Stopped")
}
