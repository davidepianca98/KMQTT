package mqtt

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestTopicValidation {

    @Test
    fun testTopicValidation() {
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
}
