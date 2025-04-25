package io.github.davidepianca98.mqtt

import io.github.davidepianca98.sha1
import io.github.davidepianca98.toBase64
import io.github.davidepianca98.validateUTF8String
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class CommonUtilsTest {

    @Test
    fun testValidate() {
        // Test NULL
        assertFalse { ubyteArrayOf(0x00u, 0x00u).toByteArray().decodeToString().validateUTF8String() }

        // Test control characters
        assertFalse { ubyteArrayOf(0x00u, 0x01u).toByteArray().decodeToString().validateUTF8String() }
        assertFalse { ubyteArrayOf(0x00u, 0x0Cu).toByteArray().decodeToString().validateUTF8String() }
        assertFalse { ubyteArrayOf(0x00u, 0x1Au).toByteArray().decodeToString().validateUTF8String() }
        assertFalse { ubyteArrayOf(0x00u, 0x1Fu).toByteArray().decodeToString().validateUTF8String() }

        // Test control characters
        assertFalse { ubyteArrayOf(0x00u, 0x7Fu).toByteArray().decodeToString().validateUTF8String() }
        assertFalse { ubyteArrayOf(0x00u, 0x8Cu).toByteArray().decodeToString().validateUTF8String() }
        assertFalse { ubyteArrayOf(0x00u, 0x9Fu).toByteArray().decodeToString().validateUTF8String() }

        // Noncharacters
        assertFalse { ubyteArrayOf(0xFFu, 0xFEu).toByteArray().decodeToString().validateUTF8String() }
        assertFalse { ubyteArrayOf(0xFFu, 0xFFu).toByteArray().decodeToString().validateUTF8String() }
        assertFalse { ubyteArrayOf(0x01u, 0xFFu, 0xFEu).toByteArray().decodeToString().validateUTF8String() }
        assertFalse { ubyteArrayOf(0x01u, 0xFFu, 0xFFu).toByteArray().decodeToString().validateUTF8String() }
        assertFalse { ubyteArrayOf(0x10u, 0xFFu, 0xFEu).toByteArray().decodeToString().validateUTF8String() }
        assertFalse { ubyteArrayOf(0x10u, 0xFFu, 0xFFu).toByteArray().decodeToString().validateUTF8String() }

        assertTrue { ubyteArrayOf(0xEFu, 0xBBu, 0xBFu).toByteArray().decodeToString().validateUTF8String() }
    }

    @Test
    fun testSHA1() {
        val str1 = "The quick brown fox jumps over the lazy dog".encodeToByteArray().sha1()
        assertEquals("L9ThxnotKPzthJ7hu3bnORuT6xI=", str1.toBase64())

        val str2 = "The quick brown fox jumps over the lazy cog".encodeToByteArray().sha1()
        assertEquals("3p8sf9JeGzr60+haC9F9mxANtLM=", str2.toBase64())

        val str3 = "".encodeToByteArray().sha1()
        assertEquals("2jmj7l5rSw0yVb/vlWAYkK/YBwk=", str3.toBase64())
    }
}
