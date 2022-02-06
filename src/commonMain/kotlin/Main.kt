import mqtt.broker.Broker
import socket.tls.TLSSettings

fun main(args: Array<String>) {
    // TODO this is here only because of NodeJS tests, that run main. As in mocha settings it's not possible to set --exit
    //      we need to avoid starting the broker, otherwise the test task won't ever terminate
    if (args.isEmpty()) {
        println("At least one argument needed:")
        println("    -h x.x.x.x")
        println("    -p port")
        println("    --max-connections n")
        println("    --key-store path")
        println("    --key-store-psw password")
        println("    --wsp port")
        return
    }
    println("Starting KMQTT")

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
        }
        i++
    }

    val host = argumentsMap["host"] ?: "0.0.0.0"
    val port = argumentsMap["port"]?.toInt() ?: 1883
    val backlog = argumentsMap["maxConn"]?.toInt() ?: 128
    val tlsSettings = argumentsMap["keyStore"]?.let {
        TLSSettings(keyStoreFilePath = it, keyStorePassword = argumentsMap["keyStorePassword"])
    }
    val wsPort = argumentsMap["wsPort"]?.toInt()

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
