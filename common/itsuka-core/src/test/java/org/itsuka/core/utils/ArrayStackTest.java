package org.itsuka.core.utils;

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
}