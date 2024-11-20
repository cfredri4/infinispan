package org.infinispan.commons.jdkspecific;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.spi.VirtualThreadCreator;
import org.infinispan.commons.util.Util;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 11.0
 **/
public class ThreadCreator {

   private static final VirtualThreadCreator VIRTUAL_THREAD_CREATOR;
   private static final Throwable VIRTUAL_THREAD_UNAVAILABILITY_CAUSE;
   static {
       VirtualThreadCreator creator;
       Throwable cause;
       try {
          creator = Util.getInstance("org.infinispan.commons.jdk21.VirtualThreadCreatorImpl", ThreadCreator.class.getClassLoader());
          cause = null;
       } catch (Throwable t) {
          creator = null;
          cause = t;
       }
       VIRTUAL_THREAD_CREATOR = creator;
       VIRTUAL_THREAD_UNAVAILABILITY_CAUSE = cause;
   }

   private static boolean useVirtualThreads;
   static {
      useVirtualThreads(Boolean.getBoolean("org.infinispan.threads.virtual"));
   }

   public static void useVirtualThreads(boolean useVirtualThreads) {
      if (useVirtualThreads) {
         if (VIRTUAL_THREAD_CREATOR != null) {
            ThreadCreator.useVirtualThreads = true;
            Log.CONTAINER.infof("Virtual threads support enabled");
         } else {
            Log.CONTAINER.debugf("Could not initialize virtual threads", VIRTUAL_THREAD_UNAVAILABILITY_CAUSE);
         }
      } else {
         ThreadCreator.useVirtualThreads = false;
      }
   }

   public static boolean useVirtualThreads() {
      return ThreadCreator.useVirtualThreads;
   }

   public static Thread createThread(ThreadGroup threadGroup, Runnable target, boolean useVirtualThreads) {
      return useVirtualThreads && VIRTUAL_THREAD_CREATOR != null
            ? VIRTUAL_THREAD_CREATOR.newVirtualThread(target)
            : new Thread(threadGroup, target);
   }

   public static Optional<ExecutorService> createBlockingExecutorService(boolean useVirtualThreads) {
      return useVirtualThreads && VIRTUAL_THREAD_CREATOR != null
            ? Optional.of(VIRTUAL_THREAD_CREATOR.newVirtualThreadPerTaskExecutor())
            : Optional.empty();
   }
}
