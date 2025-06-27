package org.itsuka.core.utils;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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