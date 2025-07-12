package org.itsuka.core.algorithm.sort;

public class InsertionSort implements Sort {
    @Override
    public void sort(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            int j = i - 1;
            int value = arr[i];
            for (; j >= 0 ; j--) {
                if (arr[j] < value) {
                    arr[j + 1] = arr[j];
                } else {
                    break;
                }
            }
            arr[j + 1] = value;
        }
    }
}
