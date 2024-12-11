package io.github.davidepianca98.datastructures

import io.github.davidepianca98.mqtt.Subscription
import kotlin.test.Test
import kotlin.test.assertEquals

class TrieTest {

    @Test
    fun testMatchesWildcardHash() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("sport/#"), "test")
        assertEquals(1, subscriptions.match("sport/test").count())
    }

    @Test
    fun testMatchesWildcardHash2() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("sport/#"), "test")
        assertEquals(1, subscriptions.match("sport/test/1/2").count())
    }

    @Test
    fun testMatchesWildcardPlus() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("sport/+"), "test")
        assertEquals(1, subscriptions.match("sport/test").count())
    }

    @Test
    fun testMatchesWildcardPlus2() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("sport/+"), "test")
        assertEquals(0, subscriptions.match("sport/test/1/2").count())
    }

    @Test
    fun testMatchesWildcardPlus3() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("sport/+/1/2"), "test")
        assertEquals(1, subscriptions.match("sport/test/1/2").count())
    }

    @Test
    fun testMatchesWildcardPlus4() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("+"), "test")
        assertEquals(1, subscriptions.match("sport").count())
    }

    @Test
    fun testMatchesWildcardPlus5() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("+"), "test")
        assertEquals(0, subscriptions.match("sport/1").count())
    }

    @Test
    fun testMatchesSame() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("/test0"), "test")
        assertEquals(1, subscriptions.match("/test0").count())
    }

    @Test
    fun testMatchesDifferent() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("/test1"), "test")
        assertEquals(0, subscriptions.match("/test0").count())
    }

    @Test
    fun testMatchesDifferent2() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("/test01"), "test")
        assertEquals(0, subscriptions.match("/test0").count())
    }

    @Test
    fun testMatchesDifferent3() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("/test10"), "test")
        assertEquals(0, subscriptions.match("/test4").count())
    }

    @Test
    fun testMatchesDifferent4() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("/test10"), "test")
        assertEquals(0, subscriptions.match("/test22").count())
    }

    @Test
    fun testMatchesWildcardDoublePlus() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("+/+"), "test")
        assertEquals(0, subscriptions.match("TopicA").count())
    }

    @Test
    fun testMatchesWildcardHash3() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("TopicA/#"), "test")
        assertEquals(1, subscriptions.match("TopicA").count())
    }

    @Test
    fun testMatchesSame2() {
        val subscriptions = Trie()
        subscriptions.insert(Subscription("TopicA"), "test")
        assertEquals(1, subscriptions.match("TopicA").count())
    }
}
