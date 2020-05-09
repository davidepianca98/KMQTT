import mqtt.broker.Broker
import mqtt.broker.cluster.ClusterSettings
import socket.tls.TLSSettings

fun main(args: Array<String>) {

    val argumentsMap = HashMap<String, String>()
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-h" -> argumentsMap["host"] = args[++i]
            "-p" -> argumentsMap["port"] = args[++i]
            "--max-connections" -> argumentsMap["maxConn"] = args[++i]
            "--key-store" -> argumentsMap["keyStore"] = args[++i]
            "--key-store-psw" -> argumentsMap["keyStorePassword"] = args[++i]
        }
        i++
    }

    val host = argumentsMap["host"] ?: "127.0.0.1"
    val port = argumentsMap["port"]?.toInt() ?: 1883
    val backlog = argumentsMap["maxConn"]?.toInt() ?: 128
    val tlsSettings = argumentsMap["keyStore"]?.let {
        TLSSettings(keyStoreFilePath = it, keyStorePassword = argumentsMap["keyStorePassword"])
    }

    val broker = Broker(
        port = port,
        host = host,
        backlog = backlog,
        tlsSettings = tlsSettings,
        cluster = ClusterSettings()
    )

    setShutdownHook {
        broker.stop()
    }

    broker.listen()
}
