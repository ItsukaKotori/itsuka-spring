package org.itsuka.core.algorithm;

import lombok.Data;

import java.security.PublicKey;

/**
 * 顺序队列
 *
 * @param <T>
 */
@Data
public class ArrayQueue<T> {
    private Object[] array;

    private int capacity;

    private int head = 0;

    private int tail = 0;

    public ArrayQueue(int capacity) {
        this.capacity = capacity;
        this.array = new Object[capacity];
    }

    public boolean enqueue(T element) {
        if (tail == capacity) {
            // 队列已满
            // 1.head == 0
            if (head == 0) {
                return false;
            }
            // 2.head != 0 移动元素至head = 0处
            for (int i = head; i < capacity; i++) {
                array[i - head] = array[i];
            }
            tail -= head;
            head = 0;
        }
        array[tail++] = element;
        return true;
    }

    public T dequeue() {
        if (tail - head == 0) {
            return null;
        }
        return (T) array[head++];
    }

    public boolean clear() {
        for (int i = head; i <= tail; i++) {
            array[i] = null;
        }
        head = 0;
        tail = 0;
        return true;
    }
}
