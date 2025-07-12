package org.itsuka.core.algorithm.sort;

public class SelectionSort implements Sort {
    @Override
    public void sort(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            int max = arr[i];
            int maxIndex = i;
            for (int j = i; j < arr.length; j++) {
                if (arr[j] > max) {
                    max = arr[j];
                    maxIndex = j;
                }
            }
            if (maxIndex != i) {
                int temp = arr[maxIndex];
                arr[maxIndex] = arr[i];
                arr[i] = temp;
            }
        }
    }
}
