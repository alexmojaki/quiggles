package com.alexmojaki.quiggles;

import androidx.annotation.NonNull;

import java.util.Iterator;

public class Permutations implements Iterator<int[]>, Iterable<int[]> {
    private int numProduced;
    private int total;
    private int[] array;
    private boolean firstReady = false;

    Permutations(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("The n must be min. 1");
        }
        array = new int[n];
        total = factorial(n);
        reset();
    }

    private static int factorial(int x) {
        if (x <= 1) return 1;
        return x * factorial(x - 1);
    }

    private void reset() {
        numProduced = 0;
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        firstReady = false;
    }

    public boolean hasNext() {
        return numProduced < total;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public int[] next() {
        numProduced++;

        if (!firstReady) {
            firstReady = true;
            return array;
        }

        int temp;
        int j = array.length - 2;
        int k = array.length - 1;

        // Find largest index j with a[j] < a[j+1]

        for (; array[j] > array[j + 1]; j--) ;

        // Find index k such that a[k] is smallest integer
        // greater than a[j] to the right of a[j]

        for (; array[j] > array[k]; k--) ;

        // Interchange a[j] and a[k]

        temp = array[k];
        array[k] = array[j];
        array[j] = temp;

        // Put tail end of permutation after jth position in increasing order

        int r = array.length - 1;
        int s = j + 1;

        while (r > s) {
            temp = array[s];
            array[s++] = array[r];
            array[r--] = temp;
        }

        return array;
    }

    @NonNull
    @Override
    public Iterator<int[]> iterator() {
        return this;
    }
}
