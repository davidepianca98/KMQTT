package datastructures

import mqtt.Subscription
import mqtt.getSharedTopicFilter

class Trie(subscriptions: Map<String, Subscription>? = null) {

    private val root = TrieNode(Char.MIN_VALUE)

    init {
        subscriptions?.forEach {
            insert(it.value, it.key)
        }
    }

    fun insert(subscription: Subscription, clientId: String): Boolean {
        return insert(root, subscription.matchTopicFilter, 0, subscription, clientId)
    }

    private fun insert(
        node: TrieNode,
        topic: String,
        index: Int,
        subscription: Subscription,
        clientId: String
    ): Boolean {
        val character = topic.getOrNull(index)
        val childNode = node.children[character]
        if (childNode != null) {
            return insert(childNode, topic, index + 1, subscription, clientId)
        } else {
            if (character == null) {
                val replaced = node.subscriptions[clientId] != null
                node.subscriptions[clientId] = subscription
                return replaced
            } else {
                val newNode = TrieNode(character)
                node.children[character] = newNode
                return insert(newNode, topic, index + 1, subscription, clientId)
            }
        }
    }

    fun match(topic: String): Set<Map.Entry<String, Subscription>> {
        val realTopic = topic.getSharedTopicFilter() ?: topic
        return match(root, realTopic, -1, realTopic.startsWith("$"))
    }

    private fun match(
        node: TrieNode,
        topic: String,
        index: Int,
        dollarStart: Boolean
    ): Set<MutableMap.MutableEntry<String, Subscription>> {
        if (node.character == '#') {
            return if (dollarStart)
                emptySet()
            else
                node.subscriptions.entries
        } else if (topic.length == index) {
            return emptySet()
        } else if (!(node.character == '+' || node.character == topic.getOrNull(index) || node.character == Char.MIN_VALUE)) {
            return emptySet()
        } else {
            val subscriptions = mutableSetOf<MutableMap.MutableEntry<String, Subscription>>()
            var nextIndex = index + 1
            if (topic.length - 1 == index) {
                subscriptions += node.subscriptions.entries
                node.children['/']?.let {
                    it.children['#']?.let {
                        subscriptions += it.subscriptions.entries
                    }
                }
            } else if (node.character == '+') {
                if (dollarStart) {
                    return emptySet()
                } else {
                    val levels = topic.substring(index).split("/")
                    val moreLevels = levels.size > 1
                    if (!moreLevels) {
                        return node.subscriptions.entries
                    } else {
                        nextIndex = index + levels[0].length
                    }
                }
            }
            node.children.forEach {
                subscriptions += match(it.value, topic, nextIndex, dollarStart)
            }
            return subscriptions
        }
    }

    fun delete(topic: String, clientId: String): Boolean {
        if (topic.isEmpty())
            return false
        val realTopic = topic.getSharedTopicFilter() ?: topic
        return delete(root, realTopic, 0, clientId)
    }

    private fun delete(node: TrieNode, topic: String, index: Int, clientId: String): Boolean {
        val character = topic.getOrNull(index)
        val childNode = node.children[character]
        if (childNode != null) {
            val result = delete(childNode, topic, index + 1, clientId)
            if (result && childNode.children.isEmpty() && childNode.subscriptions.isEmpty()) {
                node.children.remove(character!!)
            }
            return result
        } else {
            if (topic.length == index) {
                node.subscriptions.remove(clientId)
                return true
            }
        }
        return false
    }

    fun delete(clientId: String) {
        delete(root, clientId)
    }

    private fun delete(node: TrieNode, clientId: String) {
        node.subscriptions.remove(clientId)
        val iterator = node.children.iterator()
        while (iterator.hasNext()) {
            val child = iterator.next()
            delete(child.value, clientId)
            if (child.value.children.isEmpty() && child.value.subscriptions.isEmpty()) {
                iterator.remove()
            }
        }
    }

    internal class TrieNode(
        val character: Char,
        val children: HashMap<Char, TrieNode> = HashMap(),
        val subscriptions: HashMap<String, Subscription> = HashMap()
    )
}
