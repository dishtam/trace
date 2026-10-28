package trace.agent;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Bounded, lock-free ring buffer (many producers, single logical consumer),
 * Disruptor-style per-slot sequence numbers. Overflow = drop + count;
 * offer() never blocks and never throws (instrumentation-safety rule 7).
 * A dropped event never consumes a slot, so drops cannot stall later offers.
 */
public final class RingBuffer<T> {
    private final AtomicReferenceArray<T> slots;
    private final AtomicLongArray slotSeq; // slot is free for ticket t when slotSeq == t; published when slotSeq == t+1
    private final int mask;
    private final AtomicLong nextTicket = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private long readTicket; // consumer only (poll is synchronized)

    public RingBuffer(int capacityPow2) {
        if (capacityPow2 <= 0 || (capacityPow2 & (capacityPow2 - 1)) != 0) {
            throw new IllegalArgumentException("capacity must be a power of two");
        }
        this.slots = new AtomicReferenceArray<>(capacityPow2);
        this.slotSeq = new AtomicLongArray(capacityPow2);
        for (int i = 0; i < capacityPow2; i++) slotSeq.set(i, i);
        this.mask = capacityPow2 - 1;
    }

    public boolean offer(T item) {
        while (true) {
            long ticket = nextTicket.get();
            int slot = (int) (ticket & mask);
            long seq = slotSeq.get(slot);
            if (seq > ticket) continue; // another producer claimed this ticket; retry with the next
            if (seq < ticket) { dropped.incrementAndGet(); return false; } // slot not yet freed = full
            if (nextTicket.compareAndSet(ticket, ticket + 1)) {
                slots.set(slot, item);
                slotSeq.set(slot, ticket + 1); // publish after the write
                return true;
            }
        }
    }

    /** Synchronized so a shutdown flush can safely race the writer thread. */
    public synchronized T poll() {
        int slot = (int) (readTicket & mask);
        if (slotSeq.get(slot) != readTicket + 1) return null;
        T item = slots.get(slot);
        slots.set(slot, null);
        slotSeq.set(slot, readTicket + slots.length()); // free for the next cycle
        readTicket++;
        return item;
    }

    public long droppedCount() { return dropped.get(); }
    public int capacity() { return slots.length(); }
}
