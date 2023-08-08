@file:JsModule("node:dgram")

package socket.udp

import node.buffer.Buffer
import node.events.Event
import node.events.EventType
import node.net.SocketConstructorOpts
import node.stream.Duplex

internal external interface RemoteInfo {
    var address: String
    var family: String /* "IPv4" | "IPv6" */
    var port: Number
    var size: Number
}

internal open external class Socket : Duplex {
    constructor(options: SocketConstructorOpts = definedExternally)

    /**
     * Sends data on the socket. The second parameter specifies the encoding in the
     * case of a string. It defaults to UTF8 encoding.
     *
     * Returns `true` if the entire data was flushed successfully to the kernel
     * buffer. Returns `false` if all or part of the data was queued in user memory.`'drain'` will be emitted when the buffer is again free.
     *
     * The optional `callback` parameter will be executed when the data is finally
     * written out, which may not be immediately.
     *
     * See `Writable` stream `write()` method for more
     * information.
     * @since v0.1.90
     * @param [encoding='utf8'] Only used when data is `string`.
     */
    override fun write(
        chunk: Any, /* Uint8Array | string */
        callback: (error: Error?) -> Unit,
    ): Boolean

    override fun write(
        chunk: Any, /* Uint8Array | string */
        encoding: node.buffer.BufferEncoding,
        callback: (error: Error?) -> Unit,
    ): Boolean

    open fun connect(
        port: Number,
        host: String,
        connectionListener: () -> Unit = definedExternally,
    ) /* : this */

    open fun connect(
        port: Number,
        connectionListener: () -> Unit = definedExternally,
    ) /* : this */

    open fun connect(
        path: String,
        connectionListener: () -> Unit = definedExternally,
    ) /* : this */

    /**
     * Set the encoding for the socket as a `Readable Stream`. See `readable.setEncoding()` for more information.
     * @since v0.1.90
     * @return The socket itself.
     */
    override fun setEncoding(encoding: node.buffer.BufferEncoding) /* : this */

    /**
     * Pauses the reading of data. That is, `'data'` events will not be emitted.
     * Useful to throttle back an upload.
     * @return The socket itself.
     */
    override fun pause() /* : this */

    /**
     * Resumes reading after a call to `socket.pause()`.
     * @return The socket itself.
     */
    override fun resume() /* : this */

    /**
     * events.EventEmitter
     *   1. close
     *   2. connect
     *   3. data
     *   4. drain
     *   5. end
     *   6. error
     *   7. lookup
     *   8. ready
     *   9. timeout
     */
    override fun addListener(
        event: EventType,
        listener: Function<Unit>,
    ) /* : this */

    open fun addListener(
        event: Event.CLOSE,
        listener: (hadError: Boolean) -> Unit,
    ) /* : this */

    open fun addListener(
        event: Event.CONNECT,
        listener: () -> Unit,
    ) /* : this */

    open fun addListener(
        event: Event.DATA,
        listener: (data: Buffer) -> Unit,
    ) /* : this */

    override fun addListener(
        event: Event.DRAIN,
        listener: () -> Unit,
    ) /* : this */

    override fun addListener(
        event: Event.END,
        listener: () -> Unit,
    ) /* : this */

    override fun addListener(
        event: Event.ERROR,
        listener: (error: Error) -> Unit,
    ) /* : this */

    open fun addListener(
        event: Event.LOOKUP,
        listener: (
            error: Error,
            address: String,
            family: Any, /* string | number */
            host: String,
        ) -> Unit,
    ) /* : this */

    open fun addListener(
        event: Event.READY,
        listener: () -> Unit,
    ) /* : this */

    open fun addListener(
        event: Event.TIMEOUT,
        listener: () -> Unit,
    ) /* : this */

    override fun emit(
        event: EventType,
        vararg args: Any,
    ): Boolean

    open fun emit(
        event: Event.CLOSE,
        hadError: Boolean,
    ): Boolean

    open fun emit(event: Event.CONNECT): Boolean
    open fun emit(
        event: Event.DATA,
        data: Buffer,
    ): Boolean

    override fun emit(event: Event.DRAIN): Boolean
    override fun emit(event: Event.END): Boolean
    override fun emit(
        event: Event.ERROR,
        err: Error,
    ): Boolean

    open fun emit(
        event: Event.LOOKUP,
        err: Error,
        address: String,
        family: Any, /* string | number */
        host: String,
    ): Boolean

    open fun emit(event: Event.READY): Boolean
    open fun emit(event: Event.TIMEOUT): Boolean
    override fun on(
        event: EventType,
        listener: Function<Unit>,
    ) /* : this */

    open fun on(
        event: Event.CLOSE,
        listener: (hadError: Boolean) -> Unit,
    ) /* : this */

    open fun on(
        event: Event.CONNECT,
        listener: () -> Unit,
    ) /* : this */

    open fun on(
        event: Event.DATA,
        listener: (data: Buffer) -> Unit,
    ) /* : this */

    override fun on(
        event: Event.DRAIN,
        listener: () -> Unit,
    ) /* : this */

    override fun on(
        event: Event.END,
        listener: () -> Unit,
    ) /* : this */

    override fun on(
        event: Event.ERROR,
        listener: (error: Error) -> Unit,
    ) /* : this */

    open fun on(
        event: Event.LOOKUP,
        listener: (
            error: Error,
            address: String,
            family: Any, /* string | number */
            host: String,
        ) -> Unit,
    ) /* : this */

    open fun on(
        event: Event.READY,
        listener: () -> Unit,
    ) /* : this */

    open fun on(
        event: Event.MESSAGE,
        listener: (data: Buffer, rinfo: RemoteInfo) -> Unit,
    ) /* : this */

    fun send(data: Buffer, port: Number, address: String, callback: (error: Event.ERROR?, bytes: Number) -> Unit)
}
