// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.util.collection.JBIterable;
import consulo.util.collection.JBIterator;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * @author gregsh
 */
public abstract class IndexSet<S extends Index> {
    final int[] values;

    IndexSet(int[] indices) {
        values = indices;
    }

    public int size() {
        return values.length;
    }

    public int[] asArray() {
        return values.clone();
    }

    public List<S> asList() {
        List<S> result = new ArrayList<>(values.length);
        for (int value : values) {
            result.add(forValue(value));
        }
        return result;
    }

    public JBIterable<S> asIterable() {
        return new JBIterable<>() {
            @Override
            public Iterator<S> iterator() {
                return new JBIterator<>() {
                    private int myNextValueIdx;

                    @Override
                    protected @Nullable S nextImpl() {
                        return myNextValueIdx < values.length ? forValue(values[myNextValueIdx++]) : stop();
                    }
                };
            }
        };
    }

    public S first() {
        return values.length == 0 ? forValue(-1) : forValue(values[0]);
    }

    public S last() {
        return values.length == 0 ? forValue(-1) : forValue(values[values.length - 1]);
    }

    protected abstract S forValue(int value);

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IndexSet set = (IndexSet) o;

        return Arrays.equals(values, set.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    protected static int[] convert(IntUnaryOperator converter, int... ints) {
        for (int i = 0; i < ints.length; i++) {
            ints[i] = converter.applyAsInt(ints[i]);
        }
        return ints;
    }
}
