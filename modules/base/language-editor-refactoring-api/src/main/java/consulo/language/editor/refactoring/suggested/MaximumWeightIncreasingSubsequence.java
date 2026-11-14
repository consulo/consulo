// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.suggested;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToDoubleFunction;

public final class MaximumWeightIncreasingSubsequence {
    private MaximumWeightIncreasingSubsequence() {
    }

    /**
     * Finds an increasing subsequence of element in the list with the maximum sum of {@code weightFunction} for all elements.
     *
     * @return a list of indices in {@code list} of the elements of the subsequence found.
     */
    public static <T extends Comparable<T>> int[] findMaximumWeightIncreasingSubsequence(List<T> list, ToDoubleFunction<T> weightFunction) {
        if (list.isEmpty()) {
            return new int[0];
        }
        if (list.size() == 1) {
            return new int[]{0};
        }

        // subsequenceLastButOneItems[i] is the index of the last but one item in the maximum weight subsequence
        // which ends at the element with index i or -1 if the subsequence has only one element
        // (the index of the last item is always i)
        int[] subsequenceLastButOneItems = new int[list.size()];
        Arrays.fill(subsequenceLastButOneItems, -1);

        // subsequenceWeight[i] is the weight of the maximum weight subsequence which ends at the element with index i
        double[] subsequenceWeight = new double[list.size()];

        for (int i = 0; i < list.size(); i++) {
            double elementWeight = weightFunction.applyAsDouble(list.get(i));
            if (elementWeight <= 0) {
                throw new IllegalArgumentException("Weight must be positive");
            }
            subsequenceWeight[i] = elementWeight;

            for (int j = 0; j < i; j++) {
                if (list.get(i).compareTo(list.get(j)) > 0 && subsequenceWeight[i] - elementWeight < subsequenceWeight[j]) {
                    subsequenceLastButOneItems[i] = j;
                    subsequenceWeight[i] = subsequenceWeight[j] + elementWeight;
                }
            }
        }

        List<Integer> result = new ArrayList<>();
        int index = 0;
        for (int i = 1; i < list.size(); i++) {
            if (subsequenceWeight[i] > subsequenceWeight[index]) {
                index = i;
            }
        }
        while (index >= 0) {
            result.add(index);
            index = subsequenceLastButOneItems[index];
        }
        int[] array = new int[result.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = result.get(result.size() - 1 - i);
        }
        return array;
    }
}
