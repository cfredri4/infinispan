package org.infinispan.multimap.impl.function.list;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.functional.EntryView;
import org.infinispan.multimap.impl.ExternalizerIds;
import org.infinispan.multimap.impl.ListBucket;

/**
 * Serializable function used by
 * {@link org.infinispan.multimap.impl.EmbeddedMultimapListCache#offerFirst(Object, Object)}
 * and
 * {@link org.infinispan.multimap.impl.EmbeddedMultimapListCache#offerLast(Object, Object)}
 * (Object, Object)}
 * to insert a key/value pair at the head or the tail of the multimap list
 * value.
 *
 * @author Katia Aresti
 * @see <a href="https://infinispan.org/documentation/">Marshalling of
 *      Functions</a>
 * @since 15.0
 */
public final class OfferFunction<K, V> implements ListBucketBaseFunction<K, V, Void> {
   public static final AdvancedExternalizer<OfferFunction> EXTERNALIZER = new Externalizer();
   private final Collection<V> value;
   private final boolean first;

   public OfferFunction(V value, boolean first) {
      this.value = Arrays.asList(value);
      this.first = first;
   }

   public OfferFunction(Collection<V> value, boolean first) {
      this.value = value;
      this.first = first;
   }

   @Override
   public Void apply(EntryView.ReadWriteEntryView<K, ListBucket<V>> entryView) {
      Optional<ListBucket<V>> existing = entryView.peek();
      if (existing.isPresent()) {
         ListBucket<V> newBucket = existing.get().offer(value, first);
         // don't change the cache is the value already exists. it avoids replicating a
         // no-op
         if (newBucket != null) {
            entryView.set(newBucket);
         }
      } else {
         if (first) {
            // in this case collection needs to be reversed (if it supports order)
            var copy = new ArrayList<>(value);
            Collections.reverse(copy);
            entryView.set(ListBucket.create(copy));
         } else {
            entryView.set(ListBucket.create(value));
         }
      }

      return null;
   }

   private static class Externalizer implements AdvancedExternalizer<OfferFunction> {

      @Override
      public Set<Class<? extends OfferFunction>> getTypeClasses() {
         return Collections.singleton(OfferFunction.class);
      }

      @Override
      public Integer getId() {
         return ExternalizerIds.OFFER_FUNCTION;
      }

      @Override
      public void writeObject(ObjectOutput output, OfferFunction object) throws IOException {
         output.writeInt(object.value.size());
         for (var e : object.value) {
            output.writeObject(e);
         }
         output.writeBoolean(object.first);
      }

      @Override
      public OfferFunction readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         var size = input.readInt();
         var array = new ArrayList<>(size);
         for (int i = 0; i < size; i++) {
            array.add(input.readObject());
         }
         return new OfferFunction(array, input.readBoolean());
      }
   }
}
