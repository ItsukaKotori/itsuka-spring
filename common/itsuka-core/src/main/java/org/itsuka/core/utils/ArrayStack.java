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
            // 动态扩容
            capacity = (int) Math.ceil(size * 1.5);
            Object[] newArray = new Object[capacity];
            System.arraycopy(array, 0, newArray, 0, array.length);
            array = newArray;
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
