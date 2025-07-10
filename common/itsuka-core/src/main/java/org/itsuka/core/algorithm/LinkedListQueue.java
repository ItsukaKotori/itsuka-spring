package org.itsuka.core.algorithm;

public class LinkedListQueue<T> {
    static class Node<T> {
        T value;

        Node<T> next;

        public Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;

    private Node<T> tail;

    public boolean enqueue(T element) {
        Node<T> tNode = new Node<>(element);
        if (head == null) {
            head = tNode;
            tail = tNode;
            return true;
        }
        tail = tNode;
        Node<T> current = head;
        while (current.next != null) {
            current = current.next;
        }
        current.next = tNode;
        return true;
    }

    public T dequeue() {
        T value = head.value;
        head = head.next;
        return value;
    }
}
