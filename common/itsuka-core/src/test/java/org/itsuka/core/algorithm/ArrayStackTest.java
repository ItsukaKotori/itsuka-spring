package org.itsuka.core.algorithm;

import org.itsuka.core.algorithm.ArrayStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrayStackTest {
    @Test
    void pushAndPop() {
        ArrayStack<Integer> stack = new ArrayStack<Integer>(3);
        stack.push(1);
        stack.push(2);
        stack.push(3);
        assertThrows(RuntimeException.class, () -> {
            stack.push(4);
        });
        assertEquals(3, stack.pop());
    }

    @Test
    void pushOverCapacity() {
        ArrayStack<Integer> stack = new ArrayStack<Integer>(1);
        stack.push(1);
        stack.push(2);
        assertEquals(2, stack.pop());
    }
}