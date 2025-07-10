package org.itsuka.core.algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * LRU Cache
 *
 * @param <E>
 */
@Data
public class LRUCache<E> {
    /**
     * 头节点
     */
    private Node<E> first;

    /**
     * 节点数
     */
    private int size;

    /**
     * 最大节点数
     */
    private int capacity = -1;

    public LRUCache(int capacity) {
        this.capacity = capacity;
    }

    public LRUCache() {
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Node<E> {
        private E value;

        private Node<E> next;
    }

    public void addFirst(E element) {
        Node<E> node = new Node<>(element, null);
        if (first == null) {
            first = node;
            return;
        }
        Node<E> pre = findPre(element);
        if (pre == null) {
            // 说明要加入的节点不存在
            if (size == capacity) {
                // 说明列表已满 删除尾元素
                deleteEnd();
            }
        } else {
            // 删除重复的
            deleteNext(pre);
        }
        // 在头部添加新元素
        doAddFirst(node);
    }

    private void doAddFirst(Node<E> node) {
        node.next = first;
        first = node;
        size++;
    }

    private void deleteNext(Node<E> pre) {
        pre.next = pre.next.next;
        size--;
    }

    private void deleteEnd() {
        Node<E> current = first;
        while (current.next.next != null) {
            current = current.next;
        }
        current.next = null;
        size--;
    }

    private Node<E> findPre(E element) {
        // 遍历节点，查看是否已存在此节点
        // 若找到 则返回其前置节点
        Node<E> current = first;
        while (current.next != null) {
            if (Objects.equals(element, current.next.value)) {
                return current;
            }
            current = current.next;
        }
        return null;
    }
}
