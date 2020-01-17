package mqtt

class Session {

    fun clean() {

    }
}

/*
The existence of a Session, even if the rest of the Session State is empty.

The Clients subscriptions, including any Subscription Identifiers.

QoS 1 and QoS 2 messages which have been sent to the Client, but have not been completely acknowledged.

QoS 1 and QoS 2 messages pending transmission to the Client and OPTIONALLY QoS 0 messages pending transmission to the Client.

QoS 2 messages which have been received from the Client, but have not been completely acknowledged.The Will Message and the Will Delay Interval

If the Session is currently not connected, the time at which the Session will end and Session State will be discarded.
 */
