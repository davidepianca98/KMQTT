package mqtt.broker

interface SessionPersistence {

    fun getAll(): MutableMap<String, Session>

    fun persist(clientId: String, session: Session)

    fun remove(clientId: String)
}
