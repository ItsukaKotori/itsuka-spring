package org.itsuka.core.algorithm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrayQueueTest {
    private ArrayQueue<Integer> queue = new ArrayQueue<>(3);

    @BeforeEach
    void init() {
        queue.clear();
    }


    @Test
    void enqueue() {
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);
        assertFalse(queue.enqueue(4));
        assertEquals(1, queue.dequeue());
        assertEquals(2, queue.dequeue());
        assertEquals(3, queue.dequeue());
    }

    @Test
    void dequeue() {
    }
}