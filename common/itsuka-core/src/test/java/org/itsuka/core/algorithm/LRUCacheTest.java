package org.itsuka.core.algorithm;

import org.itsuka.core.algorithm.LRUCache;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LRUCacheTest {
    @Test
    void addFirst() {
        LRUCache<Integer> cache = new LRUCache<>(3);
        cache.addFirst(1);
        cache.addFirst(2);
        cache.addFirst(3);
        cache.addFirst(4);
        assertThat(cache.getFirst().getValue() == 4).isEqualTo(true);
        cache.addFirst(3);
        assertThat(cache.getFirst().getValue() == 3).isEqualTo(true);
    }

}