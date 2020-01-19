package mqtt

fun String.containsWildcard(): Boolean {
    return this.contains("#") || this.contains("+")
}

fun String.isValidTopic(): Boolean {
    /*
    if(this.contains("#")) {
        if(this.count { it.toString().contains("#") } > 1 || (this != "#" && !this.endsWith("/#")))
            return false
    }
     */
    return this.matches(Regex("^(\\#|\\+|.+/\\+|[^#]+#|.*/\\+/.*)\$")) // TODO fix
}

fun String.matchesWildcard(wildcardTopic: String): Boolean {
    return false // TODO
}
