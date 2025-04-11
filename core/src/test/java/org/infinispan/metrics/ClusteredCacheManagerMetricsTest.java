package org.infinispan.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.infinispan.test.fwk.TestCacheManagerFactory.createClusteredCacheManager;

import java.util.Collection;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.metrics.impl.MetricsRegistry;
import org.infinispan.metrics.impl.MetricsRegistryImpl;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.TransportFlags;
import org.testng.annotations.Test;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;

/**
 * @author anistor@redhat.com
 * @since 10.1
 */
@Deprecated(forRemoval=true, since = "15.2")
@Test(groups = "functional", testName = "metrics.ClusteredCacheManagerMetricsTest")
public class ClusteredCacheManagerMetricsTest extends MultipleCacheManagersTest {

   private static final String CACHE_NAME = "MyCache";

   @Override
   protected void createCacheManagers() throws Throwable {
      GlobalConfigurationBuilder globalConfig1 = GlobalConfigurationBuilder.defaultClusteredBuilder();
      globalConfig1.cacheContainer().statistics(true)
                   .metrics().prefix("ispn").gauges(true).histograms(true).namesAsTags(true);

      ConfigurationBuilder config = getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC);
      config.statistics().enable();

      EmbeddedCacheManager cacheManager1 = createClusteredCacheManager(globalConfig1, config, new TransportFlags());
      cacheManager1.start();

      GlobalConfigurationBuilder globalConfig2 = GlobalConfigurationBuilder.defaultClusteredBuilder();
      globalConfig2.metrics().prefix("ispn").gauges(true).histograms(true).namesAsTags(true);

      EmbeddedCacheManager cacheManager2 = createClusteredCacheManager(globalConfig2, config, new TransportFlags());
      cacheManager2.start();

      registerCacheManager(cacheManager1, cacheManager2);

      defineConfigurationOnAllManagers(CACHE_NAME, config);

      manager(0).getCache(CACHE_NAME);
      manager(1).getCache(CACHE_NAME);
   }

   public void testMetricsAreRegistered() {
      MetricsRegistryImpl mc0 = (MetricsRegistryImpl) GlobalComponentRegistry.componentOf(manager(0), MetricsRegistry.class);
      MetricsRegistryImpl mc1 = (MetricsRegistryImpl) GlobalComponentRegistry.componentOf(manager(1), MetricsRegistry.class);

      GlobalConfiguration gcfg0 = manager(0).getCacheManagerConfiguration();
      Tag nodeNameTag = Tag.of(Constants.NODE_TAG_NAME, gcfg0.transport().nodeName());
      Tag cacheManagerTag = Tag.of(Constants.CACHE_MANAGER_TAG_NAME, gcfg0.cacheManagerName());

      Collection<Gauge> statsEvictions = mc0.registry().find("vendor.ispn_cache_container_stats_evictions").gauges();
      assertThat(statsEvictions).hasSize(1);
      Gauge statsEviction = statsEvictions.iterator().next();
      statsEviction.getId().getTags().contains(nodeNameTag);
      statsEviction.getId().getTags().contains(cacheManagerTag);
   }
}
