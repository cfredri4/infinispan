package org.infinispan.pubsub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

@Scope(Scopes.GLOBAL)
public class DefaultPubSubManager implements PubSubManager {

    private static final Log log = LogFactory.getLog(DefaultPubSubManager.class);

    private final Map<String, List<Subscriber>> subscribers = new ConcurrentHashMap<>();

    @Inject
    Transport transport;
    
    @Inject
    BlockingManager blockingManager;

    @Override
    public void publish(String channel, byte[] message, DeliverOrder deliverOrder) {
        transport.sendToAll(new PubSubCommand(channel, message), deliverOrder);
    }

    @Override
    public void publish(Address destination, String channel, byte[] message, DeliverOrder deliverOrder) {
        transport.sendTo(destination, new PubSubCommand(channel, message), deliverOrder);
    }

    @Override
    public Subscription subscribe(String channel, MessageListener messageListener) {
        var subscriber = new Subscriber(messageListener);
        addSubscriber(channel, subscriber);
        return () -> removeSubscriber(channel, subscriber);
    }

    CompletableFuture<Void> handleMessage(String channel, byte[] message) {

        var subscribers = this.subscribers.get(channel);
        if (subscribers == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (subscribers.size() == 1) {
            return foo(subscribers.get(0), channel, message);
        }

        return CompletableFuture.allOf(subscribers.stream()
                .map(subscriber -> foo(subscriber, channel, message))
                .toArray(CompletableFuture<?>[]::new));
    }

    private CompletableFuture<Void> foo(Subscriber subscriber, String channel, byte[] message) {
        return blockingManager.runBlocking(() -> subscriber.messageListener.onMessage(channel, message), "pubsub-message")
                .toCompletableFuture();
    }

    private void addSubscriber(String channel, Subscriber subscriber) {
        this.subscribers.compute(channel, (__, subscribers) -> {
            if (subscribers == null) {
                return List.of(subscriber);
            }
            var newSubscribers = new ArrayList<Subscriber>(subscribers.size() + 1);
            newSubscribers.addAll(subscribers);
            newSubscribers.add(subscriber);
            return Collections.unmodifiableList(newSubscribers);
        });
    }

    private void removeSubscriber(String channel, Subscriber subscriber) {
        this.subscribers.computeIfPresent(channel, (__, subscribers) -> {
            if (subscribers.size() == 1 && subscribers.get(0).equals(subscriber)) {
                return null;
            }
            var newSubscribers = new ArrayList<>(subscribers);
            newSubscribers.remove(subscriber);
            return Collections.unmodifiableList(newSubscribers);
        });
    }

    private record Subscriber(MessageListener messageListener) {
    }
}
