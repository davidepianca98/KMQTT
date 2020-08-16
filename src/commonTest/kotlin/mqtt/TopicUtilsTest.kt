package mqtt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopicUtilsTest {

    @Test
    fun testContainsWildcard() {
        assertTrue { "sport/#".containsWildcard() }
        assertTrue { "#".containsWildcard() }
        assertTrue { "+".containsWildcard() }
        assertTrue { "+/tennis/#".containsWildcard() }
        assertFalse { "sport/".containsWildcard() }
        assertFalse { "/tennis/test".containsWildcard() }
    }

    @Test
    fun testIsValidTopic() {
        assertTrue { "sport/#".isValidTopic() }
        assertTrue { "#".isValidTopic() }
        assertTrue { "sport/tennis/#".isValidTopic() }
        assertFalse { "sport/tennis#".isValidTopic() }
        assertFalse { "sport/tennis/#/ranking".isValidTopic() }

        assertTrue { "+".isValidTopic() }
        assertTrue { "+/tennis/#".isValidTopic() }
        assertFalse { "sport+".isValidTopic() }
        assertTrue { "sport/+/player1".isValidTopic() }
    }

    @Test
    fun testMatchesWildcard() {
        assertTrue { "sport/test".matchesWildcard("sport/#") }
        assertTrue { "sport/test/1/2".matchesWildcard("sport/#") }
        assertTrue { "sport/test".matchesWildcard("sport/+") }
        assertFalse { "sport/test/1/2".matchesWildcard("sport/+") }
        assertTrue { "sport/test/1/2".matchesWildcard("sport/+/1/2") }
        assertTrue { "sport".matchesWildcard("+") }
        assertFalse { "sport/1".matchesWildcard("+") }

        assertTrue { "/test0".matchesWildcard("/test0") }
        assertFalse { "/test0".matchesWildcard("/test1") }
        assertFalse { "/test0".matchesWildcard("/test01") }
        assertFalse { "/test4".matchesWildcard("/test10") }
        assertFalse { "/test22".matchesWildcard("/test10") }

        assertFalse { "TopicA".matchesWildcard("+/+") }
        assertTrue { "TopicA".matchesWildcard("TopicA/#") }
        assertTrue { "TopicA".matchesWildcard("TopicA") }
    }

    @Test
    fun testMatchesWildcardBug() {
        assertFalse { "/test22".matchesWildcard("/test10") }
    }

    @Test
    fun testIsSharedTopicFilter() {
        assertTrue { "\$share/consumer1/sport/tennis/+".isSharedTopicFilter() }
    }

    @Test
    fun testGetSharedTopicFilter() {
        assertEquals("\$share/consumer1/sport/tennis/+".getSharedTopicFilter(), "sport/tennis/+")
    }

    @Test
    fun testGetSharedTopicShareName() {
        assertEquals("\$share/consumer1/sport/tennis/+".getSharedTopicShareName(), "consumer1")
    }
}
