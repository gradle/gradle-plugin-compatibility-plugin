package org.gradle.plugin.compatibility.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConcurrentWeakIdentityHashMap")
class ConcurrentWeakIdentityHashMapTest {

    @Test
    @DisplayName("should use identity comparison for keys, not equals()")
    void shouldUseIdentityComparison() {
        ConcurrentWeakIdentityHashMap<CustomKey, String> map = new ConcurrentWeakIdentityHashMap<>();

        CustomKey key1 = new CustomKey("test");
        CustomKey key2 = new CustomKey("test");

        // These objects are equal by content but not identical
        assertThat(key1).isEqualTo(key2);
        assertThat(key1).isNotSameAs(key2);

        map.computeIfAbsent(key1, k -> "value1");
        map.computeIfAbsent(key2, k -> "value2");

        // Should have two separate entries because keys are not identical
        assertThat(map.getOrDefault(key1, null)).isEqualTo("value1");
        assertThat(map.getOrDefault(key2, null)).isEqualTo("value2");

        // Removing one key should not affect the other
        map.remove(key1);
        assertThat(map.getOrDefault(key1, null)).isNull();
        assertThat(map.getOrDefault(key2, null)).isEqualTo("value2");
    }

    @Test
    @DisplayName("should return same value for identical key references")
    void shouldReturnSameValueForIdenticalKeys() {
        ConcurrentWeakIdentityHashMap<CustomKey, String> map = new ConcurrentWeakIdentityHashMap<>();

        CustomKey key = new CustomKey("test");

        map.computeIfAbsent(key, k -> "value1");

        // Same key reference should return same value
        assertThat(map.getOrDefault(key, null)).isEqualTo("value1");
    }

    @Test
    @DisplayName("should return default value when key not found")
    void shouldReturnDefaultValueWhenKeyNotFound() {
        ConcurrentWeakIdentityHashMap<CustomKey, String> map = new ConcurrentWeakIdentityHashMap<>();

        CustomKey key = new CustomKey("test");

        assertThat(map.getOrDefault(key, "default")).isEqualTo("default");
        assertThat(map.getOrDefault(key, null)).isNull();
    }

    @Test
    @DisplayName("should only compute value once for same key")
    void shouldOnlyComputeValueOnceForSameKey() {
        ConcurrentWeakIdentityHashMap<CustomKey, Integer> map = new ConcurrentWeakIdentityHashMap<>();

        CustomKey key = new CustomKey("test");
        AtomicInteger callCount = new AtomicInteger();

        Integer result1 = map.computeIfAbsent(key, k -> {
            callCount.incrementAndGet();
            return 42;
        });

        Integer result2 = map.computeIfAbsent(key, k -> {
            callCount.incrementAndGet();
            return 99;
        });

        assertThat(result1).isEqualTo(42);
        assertThat(result2).isEqualTo(42);
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("should remove mapping for given key")
    void shouldRemoveMappingForKey() {
        ConcurrentWeakIdentityHashMap<CustomKey, String> map = new ConcurrentWeakIdentityHashMap<>();

        CustomKey key = new CustomKey("test");

        map.computeIfAbsent(key, k -> "value");
        assertThat(map.getOrDefault(key, null)).isEqualTo("value");

        String removedValue = map.remove(key);
        assertThat(removedValue).isEqualTo("value");
        assertThat(map.getOrDefault(key, null)).isNull();
    }

    @Test
    @DisplayName("should return null when removing non-existent key")
    void shouldReturnNullWhenRemovingNonExistentKey() {
        ConcurrentWeakIdentityHashMap<CustomKey, String> map = new ConcurrentWeakIdentityHashMap<>();

        CustomKey key = new CustomKey("test");

        assertThat(map.remove(key)).isNull();
    }

    @Test
    @DisplayName("should not store entries when mapping function returns null")
    void shouldNotStoreEntriesWhenMappingFunctionReturnsNull() {
        ConcurrentWeakIdentityHashMap<CustomKey, String> map = new ConcurrentWeakIdentityHashMap<>();

        CustomKey key = new CustomKey("test");

        // ConcurrentHashMap doesn't support null values, so this won't create an entry
        map.computeIfAbsent(key, k -> null);

        // The entry shouldn't exist, so the default value is returned
        assertThat(map.getOrDefault(key, "default")).isEqualTo("default");
    }

    @Test
    @DisplayName("should work with list values")
    void shouldWorkWithListValues() {
        ConcurrentWeakIdentityHashMap<CustomKey, List<String>> map = new ConcurrentWeakIdentityHashMap<>();

        CustomKey key = new CustomKey("test");

        List<String> list = map.computeIfAbsent(key, k -> new ArrayList<>());
        list.add("item1");

        // Should return the same list instance
        List<String> retrieved = map.getOrDefault(key, null);
        assertThat(retrieved).isSameAs(list);
        assertThat(retrieved).containsExactly("item1");
    }

    /**
     * A custom class with content-based equality to test identity semantics.
     */
    private static class CustomKey {
        private final String value;

        CustomKey(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomKey customKey = (CustomKey) o;
            return value.equals(customKey.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
