// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.csv;

import consulo.util.lang.StringUtil;

import java.math.BigInteger;
import java.util.function.Consumer;

public abstract class TypeMerger {
    private final String myName;
    private final Consumer<String> myConsumer;
    private final int myPriority;

    TypeMerger(String name, Consumer<String> consumer, int priority) {
        myName = name;
        myConsumer = consumer;
        myPriority = priority;
    }

    public int getPriority() {
        return myPriority;
    }

    public String getName() {
        return myName;
    }

    public boolean isSuitable(String s) {
        try {
            myConsumer.accept(s);
            return true;
        }
        catch (Exception ignore) {
        }
        return false;
    }

    public TypeMerger merge(TypeMerger toMerge) {
        int priority = toMerge.getPriority();
        if (getPriority() > priority) {
            return this;
        }
        return toMerge;
    }

    public static class StringMerger extends TypeMerger {
        public StringMerger(String name) {
            super(name, s -> {
            }, 4);
        }
    }

    public static class DoubleMerger extends TypeMerger {
        public DoubleMerger(String name) {
            //noinspection ResultOfMethodCallIgnored
            super(name, Double::valueOf, 2);
        }
    }

    public static class BigIntegerMerger extends TypeMerger {
        public BigIntegerMerger(String name) {
            super(name, s -> new BigInteger(s), 1);
        }
    }

    public static class IntegerMerger extends TypeMerger {
        public IntegerMerger(String name) {
            //noinspection ResultOfMethodCallIgnored
            super(name, Integer::valueOf, 0);
        }
    }

    public static class BooleanMerger extends TypeMerger {
        public BooleanMerger(String name) {
            super(name, v -> {
                if (!StringUtil.equalsIgnoreCase(v, "true") && !StringUtil.equalsIgnoreCase(v, "false")) {
                    throw new IllegalArgumentException();
                }
            }, 0);
        }
    }
}
