package io.github.davidepianca98.mqtt.broker.interfaces

public interface MiscCallbacks {

    /**
     * Called when the broker has started listening
     */
    public fun brokerStarted()

    /**
     * Called when the broker has stopped listening
     */
    public fun brokerStopped()
}