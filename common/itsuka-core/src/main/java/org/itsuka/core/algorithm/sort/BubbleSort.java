package org.itsuka.core.algorithm.sort;

public class BubbleSort implements Sort {
    @Override
    public void sort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n; i++) {
            boolean flag = false;
            for (int j = i; j < n - i - 1; j++) {
                if (arr[j] < arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    flag = true;
                }
                if (!flag) {
                    break;
                }
            }
        }
    }
}
