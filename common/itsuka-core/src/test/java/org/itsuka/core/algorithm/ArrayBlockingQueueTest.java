package org.itsuka.core.algorithm;

import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ArrayBlockingQueueTest {
    ArrayBlockingQueue queue = new ArrayBlockingQueue(3);

    @Test
    void put() throws InterruptedException {
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    System.out.printf(String.format("当前入队元素为：%s", i));
                    queue.put(i);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        thread.start();

        TimeUnit.SECONDS.sleep(2);

        Thread thread2 = new Thread(() -> {
            try {
                queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        thread2.start();

        TimeUnit.SECONDS.sleep(10);
    }

    @Test
    void take() throws InterruptedException {
        Thread thread2 = new Thread(() -> {
            try {
                int take = queue.take();
                System.out.println("当前取出的元素为：" + take);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        thread2.start();

        TimeUnit.SECONDS.sleep(2);

        Thread thread = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                try {
                    System.out.printf(String.format("当前入队元素为：%s", i));
                    queue.put(i);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        thread.start();

        TimeUnit.SECONDS.sleep(10);
    }
}