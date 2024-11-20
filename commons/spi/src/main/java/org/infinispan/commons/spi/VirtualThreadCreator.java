package org.infinispan.commons.spi;

import java.util.concurrent.ExecutorService;

/**
 * @since 15.0
 **/
public interface VirtualThreadCreator {

   Thread newVirtualThread(Runnable target);

   ExecutorService newVirtualThreadPerTaskExecutor();
}
