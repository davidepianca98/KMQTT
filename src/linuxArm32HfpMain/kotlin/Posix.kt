import kotlinx.cinterop.*
import platform.posix.*

actual fun send(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.send(socket.convert(), buf, len.convert(), flags)
}

actual fun recv(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.recv(socket, buf, len.convert(), flags)
}

actual fun shutdown(socket: Int): Int {
    return shutdown(socket, SHUT_WR)
}

actual fun close(socket: Int): Int {
    return platform.posix.close(socket)
}

actual fun memset(__s: CValuesRef<*>?, __c: Int, __n: ULong): CPointer<out CPointed>? {
    return platform.posix.memset(__s, __c, __n.convert())
}

actual fun sendto(
    __fd: Int,
    __buf: CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: UInt
): Long {
    return platform.posix.sendto(__fd, __buf, __n.convert(), __flags, __addr, __addr_len).convert()
}

actual fun recvfrom(
    __fd: Int,
    __buf: CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: CValuesRef<UIntVarOf<UInt>>?
): Long {
    return platform.posix.recvfrom(__fd, __buf, __n.convert(), __flags, __addr, __addr_len).convert()
}

actual fun inet_ntop(
    __af: Int,
    __cp: CValuesRef<*>?,
    __buf: CValuesRef<ByteVarOf<Byte>>?,
    __len: UInt
): CPointer<ByteVarOf<Byte>>? {
    return platform.linux.inet_ntop(__af, __cp, __buf, __len)
}

actual fun inet_pton(__af: Int, __cp: String?, __buf: CValuesRef<*>?): Int {
    return platform.linux.inet_pton(__af, __cp, __buf)
}

actual fun MemScope.sockaddrIn(sin_family: UShort, sin_port: UShort): sockaddr_in {
    val sockaddr = alloc<sockaddr_in>()
    platform.posix.memset(sockaddr.ptr, 0, sockaddr_in.size.convert())
    sockaddr.sin_family = sin_family
    sockaddr.sin_port = posix_htons(sin_port.convert()).convert()
    return sockaddr
}

actual fun MemScope.getPortFromSockaddrIn(sockaddr: sockaddr_in): UShort {
    return sockaddr.sin_port
}

actual fun currentTimeMillis(): Long {
    memScoped {
        val tv = alloc<timeval>()
        gettimeofday(tv.ptr, null)
        return (tv.tv_sec.toLong() * 1000) + (tv.tv_usec / 1000)
    }
}

actual fun getErrno(): Int = errno
actual fun getEagain(): Int = EAGAIN
actual fun getEwouldblock(): Int = EWOULDBLOCK
