package org.itsuka.core.algorithm.sort;

public class QuickSort implements Sort {
    @Override
    public void sort(int[] arr) {
        quickSort(arr, 0, arr.length - 1);
    }

    public void quickSort(int[] arr, int head, int tail) {
        if (head >= tail) {
            return;
        }
        int pivot = partition(arr, head, tail);
        quickSort(arr, head , pivot - 1);
        quickSort(arr, pivot + 1, tail);
    }

    private int partition(int[] arr, int head, int tail) {
        // 分区取tail
        int i = head - 1;
        for (int j = head; j < tail ; j++) {
            if (arr[j] > arr[tail]) {
                // 交换
                i++;
                swap(arr, i, j);
            }
        }
        swap(arr, i + 1, tail);
        return i + 1;
    }
}
