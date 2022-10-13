/*
 * Copyright 2022 Google LLC
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

/**
 * Stores objects, allowing them to be looked up by an auto-assigned integer key.
 *
 * The primary use case for this class is for C++ to store "global references" to objects without
 * exhausting the 51200 available global refs. This class helps because instead of using a global
 * ref it can simply put an object into this object and look it up later by its auto-assigned key,
 * which does not require creating a global ref for the object in question.
 *
 * This class was adapted from the {@code LongObjectHashMap} class of HPPC v0.9.1:
 * https://search.maven.org/artifact/com.carrotsearch/hppc/0.9.1/jar
 *
 * This class is thread safe and may be safely used concurrently by multiple threads.
 * This thread safety is achieved by marking each public method as {@code synchronized}.
 * Therefore, any newly-added public methods must also be marked as {@code synchronized}.
 */
public final class ObjectArena {

  /** Creates a new instance of this class. */
  public ObjectArena() {
    this(DEFAULT_EXPECTED_ELEMENTS);
  }

  /**
   * Adds an object.
   *
   * @param value the object to add; may be null.
   * @return the unique key that can later be used to retrieve the object.
   */
  public synchronized long add(Object value) {
    throw new RuntimeException("not implemented");
  }

  /**
   * Removes an object.
   *
   * If the given key is unassigned, then this method does nothing.
   *
   * @param key the key of the object to remove.
   *
   * @throws IllegalArgumentException if the given key is unassigned.
   */
  public synchronized void remove(long key) {
    throw new RuntimeException("not implemented");
  }

  /**
   * Gets an object.
   *
   * @param key the key of the object to get.
   *
   * @return the object associated with the given key.
   *
   * @throws IllegalArgumentException if the given key is unassigned.
   */
  public synchronized Object get(long key) {
    throw new RuntimeException("not implemented");
  }

  /**
   * Duplicates an entry in this object.
   *
   * The object associated with the given key will be re-added and will be assigned another
   * unique key. This method is simply a shorthand for calling {@link #get} with the given key,
   * followed by {@link #add} with that object.
   *
   * @param key the key of the entry to duplicate.
   *
   * @return the newly-generated key for the duplicate entry.
   *
   * @throws IllegalArgumentException if the given key is unassigned.
   */
  public synchronized Object dup(long key) {
    throw new RuntimeException("not implemented");
  }

  /** Returns the number of objects in this object. */
  public synchronized int size() {
    throw new RuntimeException("not implemented");
  }

}
