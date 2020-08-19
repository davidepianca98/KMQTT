import kotlinx.cinterop.*
import platform.posix.posix_htons
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.windows.*

actual fun send(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.send(socket.convert(), buf, len, flags)
}

actual fun recv(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.recv(socket.convert(), buf, len, flags)
}

actual fun shutdown(socket: Int): Int {
    return platform.posix.shutdown(socket.convert(), platform.posix.SD_SEND)
}

actual fun close(socket: Int): Int {
    return platform.posix.closesocket(socket.convert())
}

actual fun memset(__s: CValuesRef<*>?, __c: Int, __n: ULong): CPointer<out CPointed>? {
    return platform.posix.memset(__s, __c, __n)
}

actual fun sendto(
    __fd: Int,
    __buf: CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: UInt
): Long {
    return platform.posix.sendto(
        __fd.convert(),
        __buf as CValuesRef<ByteVar>?, __n.convert(), __flags, __addr, __addr_len.convert()
    ).convert()
}

actual fun recvfrom(
    __fd: Int,
    __buf: CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: CValuesRef<UIntVarOf<UInt>>?
): Long {
    return platform.posix.recvfrom(
        __fd.convert(),
        __buf as CValuesRef<ByteVar>?, __n.convert(), __flags, __addr, __addr_len as CValuesRef<IntVarOf<Int>>?
    ).convert()
}

actual fun inet_ntop(
    __af: Int,
    __cp: CValuesRef<*>?,
    __buf: CValuesRef<ByteVarOf<Byte>>?,
    __len: UInt
): CPointer<ByteVarOf<Byte>>? {
    return platform.windows.inet_ntop(__af, __cp as LPCVOID?, __buf as LPSTR?, __len.convert())
}

actual fun inet_pton(__af: Int, __cp: String?, __buf: CValuesRef<*>?): Int {
    return platform.windows.inet_pton(__af, __cp, __buf as PVOID?)
}

actual fun MemScope.sockaddrIn(sin_family: UShort, sin_port: UShort): sockaddr_in {
    val sockaddr = alloc<sockaddr_in>()
    platform.posix.memset(sockaddr.ptr, 0, sockaddr_in.size.convert())
    sockaddr.sin_family = sin_family.convert()
    sockaddr.sin_port = posix_htons(sin_port.convert()).convert()
    return sockaddr
}

actual fun MemScope.getPortFromSockaddrIn(sockaddr: sockaddr_in): UShort {
    return sockaddr.sin_port
}

actual fun currentTimeMillis(): Long {
    memScoped {
        val systemTime = alloc<FILETIME>()
        GetSystemTimeAsFileTime(systemTime.ptr)
        val millisFrom1601 = (systemTime.dwHighDateTime.toULong() shl 32) + systemTime.dwLowDateTime.toULong()
        return ((millisFrom1601 - 116444736000000000u) / 10000u).toLong()
    }
}

actual fun getErrno(): Int = WSAGetLastError()
actual fun getEagain(): Int = WSAEWOULDBLOCK
actual fun getEwouldblock(): Int = WSAEWOULDBLOCK
