package org.infinispan.pubsub;

@FunctionalInterface
public interface Subscription extends AutoCloseable {

    @Override
    void close();
}