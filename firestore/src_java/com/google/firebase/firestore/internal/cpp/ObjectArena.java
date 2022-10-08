/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.firestore.internal.cpp;

import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * Stores objects, allowing them to be looked up by an auto-assigned integer key.
 *
 * The primary use case for this class is for C++ to store "global references" to objects without
 * exhausting the 51200 available global refs. This class helps because instead of using a global
 * ref it can simply put an object into this object and look it up later by its auto-assigned key,
 * which does not require creating a global ref for the object in question.
 *
 * This class was adapted from the {@code IntObjectHashMap} class of HPPC v0.9.1:
 * https://search.maven.org/artifact/com.carrotsearch/hppc/0.9.1/jar
 *
 * This class is thread safe and may be safely used concurrently by multiple threads.
 */
public final class ObjectArena {

  /** The default number of expected elements for containers. */
  private static final int DEFAULT_EXPECTED_ELEMENTS = 4;

  /** Default load factor. */
  private static final float DEFAULT_LOAD_FACTOR = 0.75f;

  /** Minimal sane load factor (99 empty slots per 100). */
  private static final float MIN_LOAD_FACTOR = 1 / 100.0f;

  /** Maximum sane load factor (1 empty slot per 100). */
  private static final float MAX_LOAD_FACTOR = 99 / 100.0f;

  /**
   * Maximum array size for hash containers (power-of-two and still allocable in Java, not a
   * negative int).
   */
  private static final int MAX_HASH_ARRAY_LENGTH = 0x80000000 >>> 1;

  /** Minimum hash buffer size. */
  private static final int MIN_HASH_ARRAY_LENGTH = 4;

  private static final int PHI_C32 = 0x9e3779b9;

  /** The array holding keys. */
  private int[] keys;

  /** The array holding values. */
  private Object[] values;

  /**
   * The number of stored keys (assigned key slots), excluding the special "empty" key, if any (use
   * {@link #size()} instead).
   *
   * @see #size()
   */
  private int assigned;

  /** Mask for slot scans in {@link #keys}. */
  private int mask;

  /** Expand (rehash) {@link #keys} when {@link #assigned} hits this value. */
  private int resizeAt;

  /** Special treatment for the "empty slot" key marker. */
  private boolean hasEmptyKey;

  /** The load factor for {@link #keys}. */
  private final double loadFactor;

  /** The next key to use. */
  private int nextKey;

  /** New instance with sane defaults. */
  public ObjectArena() {
    this(DEFAULT_EXPECTED_ELEMENTS);
  }

  /**
   * New instance with sane defaults.
   *
   * @param expectedElements The expected number of elements guaranteed not to cause buffer
   *     expansion (inclusive).
   */
  private ObjectArena(int expectedElements) {
    this(expectedElements, DEFAULT_LOAD_FACTOR);
  }

  /**
   * New instance with the provided defaults.
   *
   * @param expectedElements The expected number of elements guaranteed not to cause a rehash
   *     (inclusive).
   * @param loadFactor The load factor for internal buffers. Insane load factors (zero, full
   *     capacity) are rejected by {@link #verifyLoadFactor(double)}.
   */
  private ObjectArena(int expectedElements, double loadFactor) {
    this.loadFactor = verifyLoadFactor(loadFactor);
    ensureCapacity(expectedElements);
  }

  /**
   * Adds an object.
   *
   * @param value the object to add; may be null.
   * @return the unique key that can later be used to retrieve the object.
   *
   * @see #get
   * @see #remove
   */
  public synchronized int add(Object value) {
    assert assigned < mask + 1;

    final int key = this.nextKey++;
    final int mask = this.mask;

    if (key == 0) {
      hasEmptyKey = true;
      values[mask + 1] = value;
      return key;
    }

    final int[] keys = this.keys;
    int slot = hashKey(key) & mask;

    int existing;
    while (!((existing = keys[slot]) == 0)) {
      if (key == existing) {
        values[slot] = value;
        return key;
      }
      slot = (slot + 1) & mask;
    }

    if (assigned == resizeAt) {
      allocateThenInsertThenRehash(slot, key, value);
    } else {
      keys[slot] = key;
      values[slot] = value;
    }

    assigned++;
    return key;
  }

  /**
   * Removes an object.
   *
   * If the given key is unassigned, then this method does nothing.
   *
   * @param key the key of the object to remove.
   *
   * @throws IllegalArgumentException if the given key is unassigned.
   *
   * @see #add
   * @see #get
   */
  public synchronized void remove(int key) {
    final int mask = this.mask;

    if (key == 0) {
      if (! hasEmptyKey) {
        throw new IllegalArgumentException("key is not assigned: " + key);
      }
      hasEmptyKey = false;
      values[mask + 1] = null;
      return;
    }

    final int[] keys = this.keys;
    int slot = hashKey(key) & mask;

    int existing;
    while (!((existing = keys[slot]) == 0)) {
      if (key == existing) {
        shiftConflictingKeys(slot);
        return;
      }
      slot = (slot + 1) & mask;
    }

    throw new IllegalArgumentException("key is not assigned: " + key);
  }

  /**
   * Gets an object.
   *
   * @param key the key of the object to get.
   *
   * @return the object associated with the given key.
   *
   * @throws IllegalArgumentException if the given key is unassigned.
   *
   * @see #add
   * @see #remove
   */
  public synchronized Object get(int key) {
    final int mask = this.mask;

    if (key == 0) {
      if (! hasEmptyKey) {
        throw new IllegalArgumentException("key is not assigned: " + key);
      }
      return values[mask + 1];
    }

    final int[] keys = this.keys;
    int slot = hashKey(key) & mask;

    int existing;
    while (!((existing = keys[slot]) == 0)) {
      if (key == existing) {
        return values[slot];
      }
      slot = (slot + 1) & mask;
    }

    throw new IllegalArgumentException("key is not assigned: " + key);
  }

  /** Returns the number of objects in this object. */
  private int size() {
    return assigned + (hasEmptyKey ? 1 : 0);
  }

  /** Returns whether this object is empty. */
  private boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Ensure this container can hold at least the given number of keys (entries) without resizing its
   * buffers.
   *
   * @param expectedElements The total number of keys, inclusive.
   */
  private void ensureCapacity(int expectedElements) {
    if (expectedElements > resizeAt || keys == null) {
      final int[] prevKeys = this.keys;
      final Object[] prevValues = this.values;
      allocateBuffers(minBufferSize(expectedElements, loadFactor));
      if (prevKeys != null && !isEmpty()) {
        rehash(prevKeys, prevValues);
      }
    }
  }

  private static int minBufferSize(int elements, double loadFactor) {
    if (elements < 0) {
      throw new IllegalArgumentException("Number of elements must be >= 0: " + elements);
    }

    long length = (long) Math.ceil(elements / loadFactor);
    if (length == elements) {
      length++;
    }
    length = Math.max(MIN_HASH_ARRAY_LENGTH, nextHighestPowerOfTwo(length));

    if (length > MAX_HASH_ARRAY_LENGTH) {
      throw new BufferAllocationException(
          "Maximum array size exceeded for this load factor " + "(elements: %d, load factor: %f)",
          elements, loadFactor);
    }

    return (int) length;
  }

  /**
   * returns the next highest power of two, or the current value if it's already a power of two or
   * zero
   */
  private static long nextHighestPowerOfTwo(long v) {
    v--;
    v |= v >> 1;
    v |= v >> 2;
    v |= v >> 4;
    v |= v >> 8;
    v |= v >> 16;
    v |= v >> 32;
    v++;
    return v;
  }

  /**
   * returns the next highest power of two, or the current value if it's already a power of two or
   * zero
   */
  private static int nextHighestPowerOfTwo(int v) {
    v--;
    v |= v >> 1;
    v |= v >> 2;
    v |= v >> 4;
    v |= v >> 8;
    v |= v >> 16;
    v++;
    return v;
  }

  /**
   * Returns a hash code for the given key.
   *
   * <p>The output from this function should evenly distribute keys across the entire integer range.
   */
  private int hashKey(int key) {
    assert key != 0; // Handled as a special case (empty slot marker).
    return mixPhi(key);
  }

  private static int mixPhi(int k) {
    final int h = k * PHI_C32;
    return h ^ (h >>> 16);
  }

  /**
   * Validate load factor range and return it. Override and suppress if you need insane load
   * factors.
   */
  private double verifyLoadFactor(double loadFactor) {
    checkLoadFactor(loadFactor, MIN_LOAD_FACTOR, MAX_LOAD_FACTOR);
    return loadFactor;
  }

  private static void checkLoadFactor(
      double loadFactor, double minAllowedInclusive, double maxAllowedInclusive) {
    if (loadFactor < minAllowedInclusive || loadFactor > maxAllowedInclusive) {
      throw new BufferAllocationException(
          "The load factor should be in range [%.2f, %.2f]: %f",
          minAllowedInclusive, maxAllowedInclusive, loadFactor);
    }
  }

  /** Rehash from old buffers to new buffers. */
  private void rehash(int[] fromKeys, Object[] fromValues) {
    assert fromKeys.length == fromValues.length && checkPowerOfTwo(fromKeys.length - 1);

    // Rehash all stored key/value pairs into the new buffers.
    final int[] keys = this.keys;
    final Object[] values = this.values;
    final int mask = this.mask;
    int existing;

    // Copy the zero element's slot, then rehash everything else.
    int from = fromKeys.length - 1;
    keys[keys.length - 1] = fromKeys[from];
    values[values.length - 1] = fromValues[from];
    while (--from >= 0) {
      if (!((existing = fromKeys[from]) == 0)) {
        int slot = hashKey(existing) & mask;
        while (!((keys[slot]) == 0)) {
          slot = (slot + 1) & mask;
        }
        keys[slot] = existing;
        values[slot] = fromValues[from];
      }
    }
  }

  private static boolean checkPowerOfTwo(int arraySize) {
    // These are internals, we can just assert without retrying.
    assert arraySize > 1;
    assert nextHighestPowerOfTwo(arraySize) == arraySize;
    return true;
  }

  /**
   * Allocate new internal buffers. This method attempts to allocate and assign internal buffers
   * atomically (either allocations succeed or not).
   */
  private void allocateBuffers(int arraySize) {
    assert Integer.bitCount(arraySize) == 1;

    // Ensure no change is done if we hit an OOM.
    int[] prevKeys = this.keys;
    Object[] prevValues = this.values;
    try {
      int emptyElementSlot = 1;
      this.keys = new int[arraySize + emptyElementSlot];
      this.values = new Object[arraySize + emptyElementSlot];
    } catch (OutOfMemoryError e) {
      this.keys = prevKeys;
      this.values = prevValues;
      throw new BufferAllocationException(
          "Not enough memory to allocate buffers for rehashing: %,d -> %,d",
          e, this.mask + 1, arraySize);
    }

    this.resizeAt = expandAtCount(arraySize, loadFactor);
    this.mask = arraySize - 1;
  }

  private static int expandAtCount(int arraySize, double loadFactor) {
    assert checkPowerOfTwo(arraySize);
    // Take care of hash container invariant (there has to be at least one empty slot to ensure
    // the lookup loop finds either the element or an empty slot).
    return Math.min(arraySize - 1, (int) Math.ceil(arraySize * loadFactor));
  }

  /**
   * This method is invoked when there is a new key/ value pair to be inserted into the buffers but
   * there is not enough empty slots to do so.
   *
   * <p>New buffers are allocated. If this succeeds, we know we can proceed with rehashing so we
   * assign the pending element to the previous buffer (possibly violating the invariant of having
   * at least one empty slot) and rehash all keys, substituting new buffers at the end.
   */
  private void allocateThenInsertThenRehash(int slot, int pendingKey, Object pendingValue) {
    assert assigned == resizeAt && ((keys[slot]) == 0) && !((pendingKey) == 0);

    // Try to allocate new buffers first. If we OOM, we leave in a consistent state.
    final int[] prevKeys = this.keys;
    final Object[] prevValues = this.values;
    allocateBuffers(nextBufferSize(mask + 1, size(), loadFactor));
    assert this.keys.length > prevKeys.length;

    // We have succeeded at allocating new data so insert the pending key/value at
    // the free slot in the old arrays before rehashing.
    prevKeys[slot] = pendingKey;
    prevValues[slot] = pendingValue;

    // Rehash old keys, including the pending key.
    rehash(prevKeys, prevValues);
  }

  private static int nextBufferSize(int arraySize, int elements, double loadFactor) {
    assert checkPowerOfTwo(arraySize);
    if (arraySize == MAX_HASH_ARRAY_LENGTH) {
      throw new BufferAllocationException(
          "Maximum array size exceeded for this load factor (elements: %d, load factor: %f)",
          elements, loadFactor);
    }

    return arraySize << 1;
  }

  /**
   * Shift all the slot-conflicting keys and values allocated to (and including) <code>slot</code>.
   */
  private void shiftConflictingKeys(int gapSlot) {
    final int[] keys = this.keys;
    final Object[] values = this.values;
    final int mask = this.mask;

    // Perform shifts of conflicting keys to fill in the gap.
    int distance = 0;
    while (true) {
      final int slot = (gapSlot + (++distance)) & mask;
      final int existing = keys[slot];
      if (((existing) == 0)) {
        break;
      }

      final int idealSlot = hashKey(existing);
      final int shift = (slot - idealSlot) & mask;
      if (shift >= distance) {
        // Entry at this position was originally at or before the gap slot.
        // Move the conflict-shifted entry to the gap's position and repeat the procedure
        // for any entries to the right of the current position, treating it
        // as the new gap.
        keys[gapSlot] = existing;
        values[gapSlot] = values[slot];
        gapSlot = slot;
        distance = 0;
      }
    }

    // Mark the last found gap slot without a conflict as empty.
    keys[gapSlot] = 0;
    values[gapSlot] = null;
    assigned--;
  }

  private static class BufferAllocationException extends RuntimeException {
    BufferAllocationException(String message) {
      super(message);
    }

    BufferAllocationException(String message, Object... args) {
      this(message, null, args);
    }

    BufferAllocationException(String message, Throwable t, Object... args) {
      super(formatMessage(message, t, args), t);
    }

    private static String formatMessage(String message, Throwable t, Object... args) {
      try {
        return String.format(Locale.ROOT, message, args);
      } catch (IllegalFormatException e) {
        BufferAllocationException substitute =
            new BufferAllocationException(message + " [ILLEGAL FORMAT, ARGS SUPPRESSED]");
        if (t != null) {
          substitute.addSuppressed(t);
        }
        substitute.addSuppressed(e);
        throw substitute;
      }
    }
  }
}
