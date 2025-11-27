package org.gradle.plugin.compatibility.internal;

import org.jspecify.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A concurrent map implementation that uses weak references with identity comparison for keys.
 * Keys are compared using identity (==) rather than equals(), and are held weakly so they
 * can be garbage collected when no longer referenced elsewhere.
 * <p>
 * The JDK only provides {@link java.util.WeakHashMap} or {@link java.util.IdentityHashMap} but not both at the same
 * time. This class only implements the bare minimum of the {@link java.util.Map} interface to support the internal uses
 * of the plugin.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
class ConcurrentWeakIdentityHashMap<K, V> {
    private final ConcurrentHashMap<WeakKeyReference, V> map = new ConcurrentHashMap<>();
    private final ReferenceQueue<K> cleanupQueue = new ReferenceQueue<>();

    /**
     * Returns the value associated with the given key, or the default value if no mapping exists.
     * Automatically cleans up stale references before performing the lookup.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the value to return if no mapping exists
     * @return the value associated with the key, or the default value
     */
    public V getOrDefault(K key, V defaultValue) {
        cleanStaleRefs();
        return map.getOrDefault(new WeakKeyReference(key), defaultValue);
    }

    /**
     * If the specified key is not already associated with a value, attempts to compute its value
     * using the given mapping function and enters it into this map.
     * Automatically cleans up stale references before performing the operation.
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with the specified key
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        cleanStaleRefs();
        return map.computeIfAbsent(new WeakKeyReference(key), ref -> mappingFunction.apply(key));
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     * Automatically cleans up stale references before performing the removal.
     *
     * @param key the key whose mapping is to be removed from the map
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public @Nullable V remove(K key) {
        cleanStaleRefs();
        return map.remove(new WeakKeyReference(key));
    }

    /**
     * Removes all stale references (keys that have been garbage collected) from the map.
     */
    private void cleanStaleRefs() {
        Reference<? extends K> ref;
        while ((ref = cleanupQueue.poll()) != null) {
            //noinspection SuspiciousMethodCalls
            map.remove(ref);
        }
    }

    /**
     * A WeakReference wrapper that uses identity-based comparison for keys.
     * Two WeakKeyReferences are equal if they reference the same object (by identity, not equals).
     * The identity hash code is cached to remain stable even after the referent is garbage collected.
     */
    private class WeakKeyReference extends WeakReference<K> {
        private final int identityHashCode;

        WeakKeyReference(K key) {
            super(key, cleanupQueue);
            this.identityHashCode = System.identityHashCode(key);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }
            K thisKey = get();
            @SuppressWarnings("unchecked")
            Object otherKey = ((WeakKeyReference) obj).get();
            return thisKey != null && thisKey == otherKey;
        }

        @Override
        public int hashCode() {
            return identityHashCode;
        }
    }
}
