import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.MemScope
import platform.posix.sockaddr
import platform.posix.sockaddr_in

expect fun send(
    socket: Int,
    buf: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>?,
    len: Int,
    flags: Int
): Int

expect fun recv(
    socket: Int,
    buf: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>?,
    len: Int,
    flags: Int
): Int

expect fun shutdown(socket: Int): Int

expect fun close(socket: Int): Int

expect fun memset(
    __s: kotlinx.cinterop.CValuesRef<*>?,
    __c: Int,
    __n: ULong
): kotlinx.cinterop.CPointer<out kotlinx.cinterop.CPointed>?

expect fun sendto(
    __fd: Int,
    __buf: kotlinx.cinterop.CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: kotlinx.cinterop.CValuesRef<platform.posix.sockaddr>?,
    __addr_len: UInt
): Long

expect fun recvfrom(
    __fd: Int,
    __buf: kotlinx.cinterop.CValuesRef<*>?,
    __n: ULong,
    __flags: Int,
    __addr: kotlinx.cinterop.CValuesRef<platform.posix.sockaddr>?,
    __addr_len: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.UIntVarOf<UInt>>?
): Long

expect fun inet_ntop(
    __af: Int,
    __cp: kotlinx.cinterop.CValuesRef<*>?,
    __buf: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.ByteVarOf<Byte>>?,
    __len: UInt
): kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVarOf<Byte>>?

expect fun inet_pton(__af: Int, __cp: String?, __buf: kotlinx.cinterop.CValuesRef<*>?): Int

expect fun MemScope.sockaddrIn(sin_family: UShort, sin_port: UShort): sockaddr_in

expect fun MemScope.getPortFromSockaddrIn(sockaddr: sockaddr_in): UShort

expect fun setsockopt(
    __fd: Int,
    __level: Int,
    __optname: Int,
    __optval: kotlinx.cinterop.CValuesRef<*>?,
    __optlen: UInt
): Int

expect fun bind(__fd: Int, __addr: kotlinx.cinterop.CValuesRef<platform.posix.sockaddr>?, __len: UInt): Int

expect fun set_non_blocking(__fd: Int): Int

expect fun socket(__domain: Int, __type: Int, __protocol: Int): Int

expect fun connect(__fd: Int, __addr: CValuesRef<sockaddr>?, __len: UInt): Int

expect fun accept(
    __fd: Int,
    __addr: kotlinx.cinterop.CValuesRef<platform.posix.sockaddr>?,
    __addr_len: kotlinx.cinterop.CValuesRef<kotlinx.cinterop.UIntVarOf<UInt>>?
): Int

expect fun listen(__fd: Int, __n: Int): Int

expect fun MemScope.select(
    __nfds: Int,
    __readfds: kotlinx.cinterop.CValuesRef<platform.posix.fd_set>?,
    __writefds: kotlinx.cinterop.CValuesRef<platform.posix.fd_set>?,
    __exceptfds: kotlinx.cinterop.CValuesRef<platform.posix.fd_set>?,
    timeout: Long
): Int

expect fun socketsInit()

expect fun socketsCleanup()

expect fun getErrno(): Int
expect fun getEagain(): Int
expect fun getEwouldblock(): Int

typealias socklen_tVar = kotlinx.cinterop.UIntVarOf<UInt>
