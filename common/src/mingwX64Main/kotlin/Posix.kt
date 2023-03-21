import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.SOL_SOCKET
import platform.posix.SO_RCVTIMEO
import platform.windows.*
import platform.windows.WSAEWOULDBLOCK
import platform.windows.WSAGetLastError
import socket.IOException

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
    platform.posix.memset(sockaddr.ptr, 0, sizeOf<sockaddr_in>().convert())
    sockaddr.sin_family = sin_family.convert()
    sockaddr.sin_port = posix_htons(sin_port.convert()).convert()
    return sockaddr
}

actual fun MemScope.getPortFromSockaddrIn(sockaddr: sockaddr_in): UShort {
    return sockaddr.sin_port
}

actual fun setsockopt(__fd: Int, __level: Int, __optname: Int, __optval: CValuesRef<*>?, __optlen: UInt): Int {
    return platform.posix.setsockopt(__fd.convert(), __level, __optname, __optval.toString(), __optlen.convert())
}

actual fun bind(__fd: Int, __addr: CValuesRef<sockaddr>?, __len: UInt): Int {
    return platform.posix.bind(__fd.convert(), __addr, __len.toInt())
}

actual fun set_non_blocking(__fd: Int): Int {
    memScoped {
        val on = alloc<uint32_tVar>()
        on.value = 1u
        return platform.posix.ioctlsocket(__fd.toULong(), platform.posix.FIONBIO.toInt(), on.ptr.reinterpret())
    }
}

actual fun socket(__domain: Int, __type: Int, __protocol: Int): Int {
    return platform.posix.socket(__domain, __type, __protocol).toInt()
}

actual fun connect(__fd: Int, __addr: CValuesRef<sockaddr>?, __len: UInt): Int {
    return platform.posix.connect(__fd.convert(), __addr, __len.toInt())
}

actual fun accept(
    __fd: Int,
    __addr: CValuesRef<sockaddr>?,
    __addr_len: CValuesRef<UIntVarOf<UInt>>?
): Int {
    return platform.posix.accept(__fd.convert(), __addr, __addr_len as CValuesRef<IntVarOf<Int>>?).toInt()
}

actual fun listen(__fd: Int, __n: Int): Int {
    return platform.posix.listen(__fd.convert(), __n)
}

actual fun MemScope.select(
    __nfds: Int,
    __readfds: CValuesRef<fd_set>?,
    __writefds: CValuesRef<fd_set>?,
    __exceptfds: CValuesRef<fd_set>?,
    timeout: Long
): Int {
    val timeoutStruct = alloc<timeval>()
    timeoutStruct.tv_sec = 0
    timeoutStruct.tv_usec = (timeout * 1000).toInt()
    return platform.windows.select(__nfds, __readfds, __writefds, __exceptfds, timeoutStruct.ptr)
}

actual fun socketsInit() {
    memScoped {
        val wsaData = alloc<WSADATA>()
        if (platform.posix.WSAStartup(0x0202u, wsaData.ptr) != 0)
            throw IOException("Failed WSAStartup")
    }
}

actual fun socketsCleanup() {
    platform.posix.WSACleanup()
}


actual fun getErrno(): Int = WSAGetLastError()

actual fun getEagain(): Int = WSAEWOULDBLOCK

actual fun getEwouldblock(): Int = WSAEWOULDBLOCK

actual fun MemScope.set_socket_timeout(__fd: Int, timeout: Long): Int {
    val timeoutValue = alloc<uint32_tVar>()
    timeoutValue.value = timeout.toUInt()
    return setsockopt(__fd, SOL_SOCKET, SO_RCVTIMEO, timeoutValue.ptr, sizeOf<uint32_tVar>().toUInt())
}

actual fun MemScope.getaddrinfo(name: String, service: String?): CPointer<sockaddr>? {
    val hints = alloc<addrinfo>()
    platform.posix.memset(hints.ptr, 0, sizeOf<addrinfo>().convert())
    hints.ai_family = platform.posix.AF_UNSPEC
    hints.ai_socktype = platform.posix.SOCK_STREAM
    hints.ai_protocol = platform.posix.IPPROTO_TCP
    val result = alloc<CPointerVar<addrinfo>>()
    if (getaddrinfo(name, service, hints.ptr, result.ptr) == 0) {
        return result.pointed?.ai_addr
    }
    return null
}

actual fun fdSet(fd: Int, fdSet: CValuesRef<fd_set>) {
    posix_FD_SET(fd, fdSet)
}

actual fun fdZero(fdSet: CValuesRef<fd_set>) {
    posix_FD_ZERO(fdSet)
}

actual fun fdIsSet(fd: Int, fdSet: CValuesRef<fd_set>): Int {
    return posix_FD_ISSET(fd, fdSet)
}
