
@file:JsModule("node:tls")

package io.github.davidepianca98.socket.tls

import node.buffer.Buffer
import node.net.Server
import node.net.Socket

public sealed external interface TlsOptions {

    public var pfx: Buffer?

    public var passphrase: String?

    public var requestCert: Boolean?
}

public sealed external interface ConnectionOptions {

    public var ca: Buffer?

    public var cert: Buffer?

    public var key: Buffer?

    public var passphrase: String?

    public var servername: String?
}

public external fun createServer(
    options: TlsOptions = definedExternally,
    connectionListener: (socket: Socket) -> Unit = definedExternally,
): Server

public external fun connect(port: Number, host: String = definedExternally, options: ConnectionOptions = definedExternally, secureConnectListener: () -> Unit = definedExternally): Socket

