package org.infinispan.pubsub;

@FunctionalInterface
public interface MessageListener {

    void onMessage(String channel, byte[] message) throws Exception;
}
