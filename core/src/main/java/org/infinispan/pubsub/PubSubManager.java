package org.infinispan.pubsub;

import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.transport.Address;

public interface PubSubManager {

    void publish(String channel, byte[] message, DeliverOrder deliverOrder);

    void publish(Address destination, String channel, byte[] message, DeliverOrder deliverOrder);

    Subscription subscribe(String channel, MessageListener messageListener);
}
