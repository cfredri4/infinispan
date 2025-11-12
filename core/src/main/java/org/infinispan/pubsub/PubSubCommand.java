package org.infinispan.pubsub;

import java.util.concurrent.CompletionStage;
import org.infinispan.commands.GlobalRpcCommand;
import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.NodeVersion;

@ProtoTypeId(ProtoStreamTypeIds.PUB_SUB_COMMAND)
public class PubSubCommand implements GlobalRpcCommand {

    @ProtoField(1)
    final String channel;

    @ProtoField(2)
    final byte[] message;

    @ProtoFactory
    PubSubCommand(String channel, byte[] message) {
        this.channel = channel;
        this.message = message;
    }

    @ProtoField(1)
    String getChannel() {
        return channel;
    }

    @ProtoField(2)
    byte[] getMessage() {
        return message;
    }

    @Override
    public CompletionStage<?> invokeAsync(GlobalComponentRegistry globalComponentRegistry) throws Throwable {
        return globalComponentRegistry.getComponent(DefaultPubSubManager.class).handleMessage(channel, message);
    }

    @Override
    public boolean isReturnValueExpected() {
        return false;
    }

    @Override
    public NodeVersion supportedSince() {
        return NodeVersion.SIXTEEN;
    }
}
