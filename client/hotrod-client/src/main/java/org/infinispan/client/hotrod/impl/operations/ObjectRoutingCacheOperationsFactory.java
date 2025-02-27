package org.infinispan.client.hotrod.impl.operations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.impl.VersionedOperationResponse;

import io.netty.channel.Channel;

public class ObjectRoutingCacheOperationsFactory extends DelegatingCacheOperationsFactory {
   public ObjectRoutingCacheOperationsFactory(CacheOperationsFactory delegate) {
      super(delegate);
   }

   @Override
   public <V> HotRodOperation<V> newGetOperation(Object key) {
      return new RoutingObjectOperation<>(super.newGetOperation(key), key);
   }

   @Override
   public <K, V> HotRodOperation<GetWithMetadataOperation.GetWithMetadataResult<V>> newGetWithMetadataOperation(K key, Channel preferredChannel) {
      return new RoutingObjectOperation<>(super.newGetWithMetadataOperation(key, preferredChannel), key);
   }

   @Override
   public <V> HotRodOperation<MetadataValue<V>> newRemoveOperation(Object key) {
      return new RoutingObjectOperation<>(super.newRemoveOperation(key), key);
   }

   @Override
   public <K, V> HotRodOperation<MetadataValue<V>> newPutKeyValueOperation(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      return new RoutingObjectOperation<>(super.newPutKeyValueOperation(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit), key);
   }

   @Override
   public <K, V> HotRodOperation<MetadataValue<V>> newPutIfAbsentOperation(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      return new RoutingObjectOperation<>(super.newPutIfAbsentOperation(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit), key);
   }

   @Override
   public <K, V> HotRodOperation<MetadataValue<V>> newPutIfAbsentOperation(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit, Flag... flags) {
      return new RoutingObjectOperation<>(super.newPutIfAbsentOperation(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit, flags), key);
   }

   @Override
   public <K, V> HotRodOperation<V> newReplaceOperation(K key, V valueBytes, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
      return new RoutingObjectOperation<>(super.newReplaceOperation(key, valueBytes, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit), key);
   }

   @Override
   public <K, V> HotRodOperation<VersionedOperationResponse<V>> newReplaceIfUnmodifiedOperation(K key, V value, long lifespan, TimeUnit lifespanTimeUnit, long maxIdle, TimeUnit maxIdleTimeUnit, long version) {
      return new RoutingObjectOperation<>(super.newReplaceIfUnmodifiedOperation(key, value, lifespan, lifespanTimeUnit, maxIdle, maxIdleTimeUnit, version), key);
   }

   @Override
   public <K, V> HotRodOperation<VersionedOperationResponse<V>> newRemoveIfUnmodifiedOperation(K key, long version) {
      return new RoutingObjectOperation<>(super.newRemoveIfUnmodifiedOperation(key, version), key);
   }

   @Override
   public <K> HotRodOperation<Boolean> newContainsKeyOperation(K key) {
      return new RoutingObjectOperation<>(super.newContainsKeyOperation(key), key);
   }

   @Override
   public <T> HotRodOperation<T> executeOperation(String taskName, Map<String, byte[]> marshalledParams, Object key) {
      return new RoutingObjectOperation<>(super.executeOperation(taskName, marshalledParams, key), key);
   }

   @Override
   public HotRodOperation<GetStreamStartResponse> newGetStreamStartOperation(Object key, int batchSize) {
      return new RoutingObjectOperation<>(super.newGetStreamStartOperation(key, batchSize), key);
   }

   @Override
   public HotRodOperation<PutStreamResponse> newPutStreamStartOperation(Object key, long version, long lifespan,
                                                                        TimeUnit lifespanUnit, long maxIdleTime,
                                                                        TimeUnit maxIdleTimeUnit) {
      return new RoutingObjectOperation<>(super.newPutStreamStartOperation(key, version, lifespan, lifespanUnit,
            maxIdleTime, maxIdleTimeUnit), key);
   }

   @Override
   protected DelegatingCacheOperationsFactory newFactoryFor(CacheOperationsFactory factory) {
      return new ObjectRoutingCacheOperationsFactory(factory);
   }
}
