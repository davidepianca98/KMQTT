package mqtt

interface Authorization {

    fun authorize(topicName: String): Boolean
}
