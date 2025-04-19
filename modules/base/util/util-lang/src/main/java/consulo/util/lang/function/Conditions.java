/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.util.lang.function;

import consulo.annotation.DeprecationInfo;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SoftReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author max
 */
@SuppressWarnings("unchecked")
@Deprecated
@DeprecationInfo("Use Predicates")
public class Conditions {
    private Conditions() {
    }

    public final static Condition<Object> TRUE = Condition.TRUE;
    public final static Condition<Object> FALSE = Condition.FALSE;

    public static <T> Condition<T> alwaysTrue() {
        return (Condition<T>)TRUE;
    }

    public static <T> Condition<T> alwaysFalse() {
        return (Condition<T>)FALSE;
    }

    public static <T> Condition<T> notNull() {
        return (Condition<T>)Condition.NOT_NULL;
    }

    public static <T> Condition<T> constant(boolean value) {
        return (Condition<T>)(value ? TRUE : FALSE);
    }

    public static <T> Condition<T> instanceOf(Class<?> clazz) {
        return clazz::isInstance;
    }

    public static <T> Condition<T> notInstanceOf(Class<?> clazz) {
        return t -> !clazz.isInstance(t);
    }

    public static Condition<Class> assignableTo(Class clazz) {
        return clazz::isAssignableFrom;
    }

    public static <T> Condition<T> instanceOf(Class<?>... clazz) {
        return t -> {
            for (Class<?> aClass : clazz) {
                if (aClass.isInstance(t)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static <T> Condition<T> is(T option) {
        return equalTo(option);
    }

    public static <T> Condition<T> equalTo(Object option) {
        return t -> Comparing.equal(t, option);
    }

    public static <T> Condition<T> notEqualTo(Object option) {
        return t -> !Comparing.equal(t, option);
    }

    public static <T> Condition<T> oneOf(T... options) {
        return oneOf(Arrays.asList(options));
    }

    public static <T> Condition<T> oneOf(Collection<? extends T> options) {
        return options::contains;
    }

    public static <T> Condition<T> not(Predicate<T> c) {
        if (c == TRUE) {
            return alwaysFalse();
        }
        if (c == FALSE) {
            return alwaysTrue();
        }
        if (c instanceof Not) {
            Predicate c1 = ((Not)c).c;
            return c1 instanceof Condition cond ? cond : c1::test;
        }
        return new Not<>(c);
    }

    public static <T> Condition<T> and(Condition<T> c1, Condition<T> c2) {
        return and2(c1, c2);
    }

    public static <T> Condition<T> and2(Condition<? super T> c1, Condition<? super T> c2) {
        if (c1 == TRUE || c2 == FALSE) {
            return (Condition<T>)c2;
        }
        if (c2 == TRUE || c1 == FALSE) {
            return (Condition<T>)c1;
        }
        return new And<>(c1, c2);
    }

    public static <T> Condition<T> or(Condition<T> c1, Condition<T> c2) {
        return or2(c1, c2);
    }

    public static <T> Condition<T> or2(Condition<? super T> c1, Condition<? super T> c2) {
        if (c1 == FALSE || c2 == TRUE) {
            return (Condition<T>)c2;
        }
        if (c2 == FALSE || c1 == TRUE) {
            return (Condition<T>)c1;
        }
        return new Or<>(c1, c2);
    }

    public static <A, B> Condition<A> compose(Function<? super A, B> fun, Predicate<? super B> condition) {
        return o -> condition.test(fun.apply(o));
    }

    public static <T> Condition<T> cached(Predicate<T> c) {
        return new SoftRefCache<>(c);
    }

    private static class Not<T> implements Condition<T> {
        final Predicate<T> c;

        Not(Predicate<T> c) {
            this.c = c;
        }

        @Override
        public boolean value(T value) {
            return !c.test(value);
        }
    }

    private static class And<T> implements Condition<T> {
        final Predicate<? super T> c1;
        final Predicate<? super T> c2;

        And(Predicate<? super T> c1, Predicate<? super T> c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override
        public boolean value(T object) {
            return c1.test(object) && c2.test(object);
        }
    }

    private static class Or<T> implements Condition<T> {
        final Predicate<? super T> c1;
        final Predicate<? super T> c2;

        Or(Predicate<? super T> c1, Predicate<? super T> c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override
        public boolean value(T object) {
            return c1.test(object) || c2.test(object);
        }
    }

    private static class SoftRefCache<T> implements Condition<T> {
        private final HashMap<Integer, Pair<SoftReference<T>, Boolean>> myCache = new HashMap<>();
        private final Predicate<T> myCondition;

        public SoftRefCache(Predicate<T> condition) {
            myCondition = condition;
        }

        @Override
        public final boolean value(T object) {
            int key = object.hashCode();
            Pair<SoftReference<T>, Boolean> entry = myCache.get(key);
            if (entry == null || entry.first.get() != object) {
                boolean value = myCondition.test(object);
                myCache.put(key, Pair.create(new SoftReference<>(object), value));
                return value;
            }
            else {
                return entry.second;
            }
        }
    }
}