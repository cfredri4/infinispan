package org.infinispan.commons.jdk21;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.infinispan.commons.spi.VirtualThreadCreator;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 11.0
 **/
public class VirtualThreadCreatorImpl implements VirtualThreadCreator {

   @Override
   public Thread newVirtualThread(Runnable runnable) {
      return Thread.ofVirtual().unstarted(runnable);
   }

   @Override
   public ExecutorService newVirtualThreadPerTaskExecutor() {
      return Executors.newVirtualThreadPerTaskExecutor();
   }
}
