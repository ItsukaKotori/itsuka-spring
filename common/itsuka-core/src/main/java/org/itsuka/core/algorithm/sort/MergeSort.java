package org.itsuka.core.algorithm.sort;

public class MergeSort implements Sort {
    @Override
    public void sort(int[] arr) {
        mergeSort(arr, 0, arr.length - 1);
    }

    private void mergeSort(int[] arr, int head, int tail) {
        if (head < tail) {
            int mid = head + (tail - head) / 2;
            mergeSort(arr, head, mid);
            mergeSort(arr, mid + 1, tail);
            // 合并左右两部分
            merge(arr, head, mid, tail);
        }
    }

    private void merge(int[] arr, int head, int mid, int tail) {
        int i = head;
        int j = mid + 1;
        int[] temp = new int[arr.length];
        int index = 0;
        while (i <= mid && j <= tail) {
            if (arr[i] > arr[j]) {
                temp[index++] = arr[i++];
            } else {
                temp[index++] = arr[j++];
            }
        }

        while (i <= mid) {
            temp[index++] = arr[i++];
        }
        while (j <= tail) {
            temp[index++] = arr[j++];
        }
        // copy回arr
        index = 0;
        while (head <= tail) {
            arr[head++] = temp[index++];
        }
    }
}
