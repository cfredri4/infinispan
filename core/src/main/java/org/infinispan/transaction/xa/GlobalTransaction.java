package org.infinispan.transaction.xa;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.xa.Xid;

import org.infinispan.commons.marshall.ProtoStreamTypeIds;
import org.infinispan.commons.tx.XidImpl;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.infinispan.remoting.transport.Address;

/**
 * Uniquely identifies a transaction that spans all JVMs in a cluster. This is used when replicating all modifications
 * in a transaction; the PREPARE and COMMIT (or ROLLBACK) messages have to have a unique identifier to associate the
 * changes with<br>. GlobalTransaction should be instantiated thorough {@link TransactionFactory} class,
 * as their type depends on the runtime configuration.
 *
 * @author <a href="mailto:bela@jboss.org">Bela Ban</a> Apr 12, 2003
 * @author <a href="mailto:manik@jboss.org">Manik Surtani (manik@jboss.org)</a>
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
@ProtoTypeId(ProtoStreamTypeIds.GLOBAL_TRANSACTION)
public class GlobalTransaction implements Cloneable {

   private static final AtomicLong sid = new AtomicLong(0);

   private long id;
   private Address addr;
   private int hash_code = -1;  // in the worst case, hashCode() returns 0, then increases, so we're safe here
   private boolean remote = false;
   private final boolean clientTx;
   private volatile XidImpl xid = null;
   private volatile long internalId = -1;

   public GlobalTransaction(Address addr, boolean remote) {
      this(addr, remote, false);
   }

   public GlobalTransaction(Address addr, boolean remote, boolean clientTx) {
      this.id = sid.incrementAndGet();
      this.addr = addr;
      this.remote = remote;
      this.clientTx = clientTx;
   }

   @ProtoFactory
   GlobalTransaction(long id, Address address, XidImpl xid, long internalId, boolean clientTransaction) {
      this.id = id;
      this.addr = address;
      this.xid = xid;
      this.internalId = internalId;
      this.clientTx = clientTransaction;
   }

   @ProtoField(1)
   public Address getAddress() {
      return addr;
   }

   @ProtoField(2)
   public long getId() {
      return id;
   }

   @ProtoField(3)
   public XidImpl getXid() {
      return xid;
   }

   @ProtoField(4)
   public long getInternalId() {
      return internalId;
   }

   @ProtoField(5)
   public boolean isClientTransaction() {
      return clientTx;
   }

   public boolean isRemote() {
      return remote;
   }

   public void setRemote(boolean remote) {
      this.remote = remote;
   }

   @Override
   public int hashCode() {
      if (hash_code == -1) {
         hash_code = (addr != null ? addr.hashCode() : 0) + (int) id;
      }
      return hash_code;
   }

   @Override
   public boolean equals(Object other) {
      if (this == other)
         return true;
      if (!(other instanceof GlobalTransaction otherGtx))
         return false;

      return id == otherGtx.id &&
            Objects.equals(addr, otherGtx.addr);
   }

   /**
    * Returns a simplified representation of the transaction.
    */
   public final String globalId() {
      return getAddress() + ":" + getId();
   }

   public void setId(long id) {
      this.id = id;
   }

   public void setAddress(Address address) {
      this.addr = address;
   }

   public void setXid(Xid xid) {
      this.xid = XidImpl.copy(xid);
   }

   public void setInternalId(long internalId) {
      this.internalId = internalId;
   }

   @Override
   public Object clone() {
      try {
         return super.clone();
      } catch (CloneNotSupportedException e) {
         throw new IllegalStateException("Impossible!");
      }
   }

   @Override
   public String toString() {
      return "GlobalTransaction{" +
            "id=" + id +
            ", addr=" + Objects.toString(addr, "local") +
            ", remote=" + remote +
            ", xid=" + xid +
            ", internalId=" + internalId +
            '}';
   }
}
