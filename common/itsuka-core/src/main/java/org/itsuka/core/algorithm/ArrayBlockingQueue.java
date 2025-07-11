package org.itsuka.core.algorithm;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ArrayBlockingQueue {
    private int[] array;

    private int capacity;

    private int head;

    private int tail;

    private ReentrantLock lock;

    private Condition notEmpty;

    private Condition notFull;

    public ArrayBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.array = new int[capacity];
        this.lock = new ReentrantLock();

        notEmpty = this.lock.newCondition();
        notFull = this.lock.newCondition();
    }

    public boolean put(int element) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            if (tail == capacity) {
                if (head == 0) {
                    notFull.await();
                }
            }
            if (tail == capacity && head != 0) {
                for (int i = head; i < tail ; i++) {
                    array[i - head] = array[i];
                }
                tail -= head;
                head = 0;
            }
            array[tail++] = element;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        return true;
    }

    public int take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            if (head == tail) {
                notEmpty.await();
            }
            int element = array[head++];
            notFull.signal();
            return element;
        } finally {
            lock.unlock();
        }
    }
}
