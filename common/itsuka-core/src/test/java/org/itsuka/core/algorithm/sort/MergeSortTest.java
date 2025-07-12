package org.itsuka.core.algorithm.sort;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MergeSortTest {
    @Test
    void sort() {
        int[] arr = new int[]{1, 9, 5, 4, 3, 2};
        Sort sort = new MergeSort();
        sort.sort(arr);
        System.out.println(Arrays.toString(arr));
    }
}