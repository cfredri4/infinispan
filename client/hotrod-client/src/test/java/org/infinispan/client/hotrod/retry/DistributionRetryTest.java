package org.infinispan.client.hotrod.retry;

import static org.infinispan.server.hotrod.test.HotRodTestingUtil.hotRodCacheConfiguration;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyAffinityServiceFactory;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.client.hotrod.exceptions.TransportException;
import org.infinispan.client.hotrod.test.HotRodClientTestingUtil;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.remoting.transport.Address;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.junit.jupiter.api.Assertions;
import org.testng.annotations.Test;

/**
 * @author Mircea.Markus@jboss.com
 * @since 4.1
 */
@CleanupAfterMethod
@Test(testName = "client.hotrod.retry.DistributionRetryTest", groups = "functional")
public class DistributionRetryTest extends AbstractRetryTest {

   private int retries = 0;

   @Override
   protected ConfigurationBuilder getCacheConfig() {
      ConfigurationBuilder builder = hotRodCacheConfiguration(
            getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, false));
      builder.clustering().hash().numOwners(1);
      return builder;
   }

   @Override
   protected void amendRemoteCacheManagerConfiguration(org.infinispan.client.hotrod.configuration.ConfigurationBuilder builder) {
      builder.maxRetries(retries);
   }

   private boolean nextOperationShouldFail() {
      return retries == 0;
   }

   private void assertOperationFailsWithTransport(Object key) {
      try {
         remoteCache.get(key);

         // If the previous didn't fail, then it has to be one of the connection failed servers
         Assertions.assertEquals(1, remoteCache.getDispatcher().getConnectionFailedServers().size());
      } catch (TransportException te) {
         // Is expected
      }

      // Connection failed servers can take a bit to be propagated as it has to fail reconnecting
      eventuallyEquals(1, () -> remoteCache.getDispatcher().getConnectionFailedServers().size());
   }

   public void testGet() throws Exception {
      Object key = generateKeyAndShutdownServer();
      log.info("Starting actual test");
      if (nextOperationShouldFail()) assertOperationFailsWithTransport(key);
      //now make sure that next call won't fail
      resetStats();
      assertEquals(remoteCache.get(key), "v");
   }

   public void testPut() throws Exception {
      Object key = generateKeyAndShutdownServer();
      log.info("Here it starts");
      if (nextOperationShouldFail()) assertOperationFailsWithTransport(key);
      assertEquals(remoteCache.put(key, "v0"), "v");
   }

   public void testRemove() throws Exception {
      Object key = generateKeyAndShutdownServer();
      if (nextOperationShouldFail()) assertOperationFailsWithTransport(key);
      assertEquals("v", remoteCache.remove(key));
   }

   public void testContains() throws Exception {
      Object key = generateKeyAndShutdownServer();
      if (nextOperationShouldFail()) assertOperationFailsWithTransport(key);
      resetStats();
      assertEquals(true, remoteCache.containsKey(key));
   }

   public void testGetWithMetadata() throws Exception {
      Object key = generateKeyAndShutdownServer();
      if (nextOperationShouldFail()) assertOperationFailsWithTransport(key);
      resetStats();
      VersionedValue value = remoteCache.getWithMetadata(key);
      assertEquals("v", value.getValue());
   }

   public void testPutIfAbsent() throws Exception {
      Object key = generateKeyAndShutdownServer();
      if (nextOperationShouldFail()) assertOperationFailsWithTransport(key);
      assertEquals(null, remoteCache.putIfAbsent("noSuchKey", "someValue"));
      assertEquals("someValue", remoteCache.get("noSuchKey"));
   }

   public void testReplace() throws Exception {
      Object key = generateKeyAndShutdownServer();
      if (nextOperationShouldFail()) assertOperationFailsWithTransport(key);
      assertEquals("v", remoteCache.replace(key, "v2"));
   }

   public void testReplaceIfUnmodified() throws Exception {
      Object key = generateKeyAndShutdownServer();
      if (nextOperationShouldFail()) assertOperationFailsWithTransport(key);
      assertEquals(false, remoteCache.replaceWithVersion(key, "v2", 12));
   }

   public void testRemoveIfUnmodified() throws Exception {
      Object key = generateKeyAndShutdownServer();
      if (nextOperationShouldFail()) assertOperationFailsWithTransport(key);
      resetStats();
      assertEquals(false, remoteCache.removeWithVersion(key, 12));
   }

   public void testClear() throws Exception {
      Object key = generateKeyAndShutdownServer();
      if (nextOperationShouldFail()) assertOperationFailsWithTransport(key);
      resetStats();
      remoteCache.clear();
      assertEquals(false, remoteCache.containsKey(key));
   }

   private Object generateKeyAndShutdownServer() throws IOException, ClassNotFoundException, InterruptedException {
      resetStats();
      Cache<Object,Object> cache = manager(0).getCache();
      ExecutorService ex = Executors.newSingleThreadExecutor(getTestThreadFactory("KeyGenerator"));
      KeyAffinityService kaf = KeyAffinityServiceFactory.newKeyAffinityService(cache, ex, new ByteKeyGenerator(), 2, true);
      Address address = cache.getAdvancedCache().getRpcManager().getTransport().getAddress();
      byte[] keyBytes = (byte[]) kaf.getKeyForAddress(address);
      String key = ByteKeyGenerator.getStringObject(keyBytes);
      ex.shutdownNow();
      kaf.stop();

      remoteCache.put(key, "v");
      assertOnlyServerHit(getAddress(hotRodServer1));

      log.info("About to stop Hot Rod server 1");
      HotRodClientTestingUtil.killServers(hotRodServer1);

      return key;
   }

   public static class ByteKeyGenerator implements KeyGenerator<Object> {
      Random r = new Random();
      @Override
      public byte[] getKey() {
         String result = String.valueOf(r.nextLong());
         try {
            return new ProtoStreamMarshaller().objectToByteBuffer(result, 64);
         } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
         }
      }

      public static String getStringObject(byte[] bytes) {
         try {
            return (String) new ProtoStreamMarshaller().objectFromByteBuffer(bytes);
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }
   }

   private DistributionRetryTest withRetries(int retries) {
      this.retries = retries;
      return this;
   }

   @Override
   protected String parameters() {
      return "[retries=" + retries + "]";
   }

   @Override
   public Object[] factory() {
      return new Object[] {
            new DistributionRetryTest().withRetries(0),
            new DistributionRetryTest().withRetries(10),
      };
   }
}
