package org.itsuka.core.utils;

/**
 * 基于数组的顺序栈
 */
public class ArrayStack<T> {
    private Object[] array;

    private int capacity;

    private int size;

    public ArrayStack(int capacity) {
        this.capacity = capacity;
        this.array = new Object[capacity];
    }

    public void push(T element) {
        if (size == capacity) {
            throw new RuntimeException("exceed capacity!");
        }
        array[size++] = element;
    }

    public T pop() {
        if (size == 0) {
            throw new RuntimeException("no element to pop!");
        }
        return (T) array[--size];
    }
}
