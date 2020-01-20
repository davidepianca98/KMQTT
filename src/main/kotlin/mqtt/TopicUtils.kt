package mqtt

fun String.containsWildcard(): Boolean {
    return this.contains("#") || this.contains("+")
}

fun String.isValidTopic(): Boolean {
    if (this.contains("#")) {
        if (this.count { it.toString().contains("#") } > 1 || (this != "#" && !this.endsWith("/#")))
            return false
    }
    if (this.contains("+")) { // Taken from Paho MQTT Java
        for (i in 0 until length) {
            if (this[i] == '+') {
                val prev = if (i - 1 >= 0) this[i - 1] else null
                val next = if (i + 1 < length) this[i + 1] else null
                if (prev != '/' && prev != null || next != '/' && next != null)
                    return false
            }
        }
    }

    return true
}

// Taken from Paho MQTT Java
fun String.matchesWildcard(wildcardTopic: String): Boolean {
    var curn = 0
    var curf = 0
    val curnEnd: Int = this.length
    val curfEnd: Int = wildcardTopic.length

    if (this.containsWildcard())
        return false
    if (!this.isValidTopic() || !wildcardTopic.isValidTopic())
        return false

    if (this == wildcardTopic) {
        return true
    }

    while (curf < curfEnd && curn < curnEnd) {
        if (this[curn] == '/' && wildcardTopic[curf] != '/')
            break
        if (wildcardTopic[curf] != '+' && wildcardTopic[curf] != '#' && wildcardTopic[curf] != this[curn])
            break
        if (wildcardTopic[curf] == '+') { // skip until we meet the next separator, or end of string
            var nextpos = curn + 1
            while (nextpos < curnEnd && this[nextpos] != '/')
                nextpos = ++curn + 1
        } else if (wildcardTopic[curf] == '#')
            curn = curnEnd - 1 // skip until end of string
        curf++
        curn++
    }

    return curn == curnEnd && curf == curfEnd
}
