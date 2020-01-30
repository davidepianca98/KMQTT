package mqtt

import validateUTF8String
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class TestCommonUtils {

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
}
