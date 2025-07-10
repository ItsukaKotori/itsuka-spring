package org.itsuka.core.algorithm;

import java.io.CharArrayReader;

public class CircularQueue<T> {
    private Object[] array;

    private int capacity;

    private int head;

    private int tail;

    public CircularQueue(int capacity) {
        this.capacity = capacity;
        this.array = new Object[capacity];
    }

    public boolean enqueue(T element) {
        // 队列满了
        if ((tail + 1) % capacity == head) {
            return false;
        }
        array[tail] = element;
        tail = (tail + 1) % capacity;
        return true;
    }

    public T dequeue() {
        // 队列为空
        if (head == tail) {
            return null;
        }
        Object element = array[head];
        head = (head + 1) % capacity;
        return (T) element;
    }
}
