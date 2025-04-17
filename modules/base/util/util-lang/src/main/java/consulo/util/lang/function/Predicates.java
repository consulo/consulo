/*
 * Copyright 2013-2024 consulo.io
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

import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SoftReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2024-05-12
 * <p>
 * Same like {@link Predicates} with {@link Predicate}
 */
@SuppressWarnings({"unchecked", "unused"})
public class Predicates {
    private Predicates() {
    }

    public final static Predicate<Object> TRUE = o -> true;
    public final static Predicate<Object> FALSE = o -> false;

    public static <T> Predicate<T> alwaysTrue() {
        return (Predicate<T>)TRUE;
    }

    public static <T> Predicate<T> alwaysFalse() {
        return (Predicate<T>)FALSE;
    }

    public static <T> Predicate<T> notNull() {
        return Objects::nonNull;
    }

    public static <T> Predicate<T> constant(boolean value) {
        return (Predicate<T>)(value ? TRUE : FALSE);
    }

    public static <T> Predicate<T> instanceOf(final Class<?> clazz) {
        return clazz::isInstance;
    }

    public static <T> Predicate<T> notInstanceOf(final Class<?> clazz) {
        return t -> !clazz.isInstance(t);
    }

    public static Predicate<Class> assignableTo(final Class clazz) {
        return clazz::isAssignableFrom;
    }

    public static <T> Predicate<T> instanceOf(final Class<?>... clazz) {
        return t -> {
            for (Class<?> aClass : clazz) {
                if (aClass.isInstance(t)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static <T> Predicate<T> is(final T option) {
        return equalTo(option);
    }

    public static <T> Predicate<T> equalTo(final Object option) {
        return t -> Comparing.equal(t, option);
    }

    public static <T> Predicate<T> notEqualTo(final Object option) {
        return t -> !Objects.equals(t, option);
    }

    public static <T> Predicate<T> oneOf(T... options) {
        return oneOf(Arrays.asList(options));
    }

    public static <T> Predicate<T> oneOf(final Collection<? extends T> options) {
        return options::contains;
    }

    public static <T> Predicate<T> not(Predicate<T> c) {
        if (c == TRUE) {
            return alwaysFalse();
        }
        if (c == FALSE) {
            return alwaysTrue();
        }
        if (c instanceof Not) {
            return ((Not)c).c;
        }
        return new Not<>(c);
    }

    public static <T> Predicate<T> and(Predicate<T> c1, Predicate<T> c2) {
        return and2(c1, c2);
    }

    public static <T> Predicate<T> and2(Predicate<? super T> c1, Predicate<? super T> c2) {
        if (c1 == TRUE || c2 == FALSE) {
            return (Predicate<T>)c2;
        }
        if (c2 == TRUE || c1 == FALSE) {
            return (Predicate<T>)c1;
        }
        return new And<>(c1, c2);
    }

    public static <T> Predicate<T> or(Predicate<T> c1, Predicate<T> c2) {
        return or2(c1, c2);
    }

    public static <T> Predicate<T> or2(Predicate<? super T> c1, Predicate<? super T> c2) {
        if (c1 == FALSE || c2 == TRUE) {
            return (Predicate<T>)c2;
        }
        if (c2 == FALSE || c1 == TRUE) {
            return (Predicate<T>)c1;
        }
        return new Or<>(c1, c2);
    }

    public static <A, B> Predicate<A> compose(final Function<? super A, B> fun, final Predicate<? super B> condition) {
        return o -> condition.test(fun.apply(o));
    }

    public static <T> Predicate<T> cached(Predicate<T> c) {
        return new SoftRefCache<>(c);
    }

    private static class Not<T> implements Predicate<T> {
        final Predicate<T> c;

        Not(Predicate<T> c) {
            this.c = c;
        }

        @Override
        public boolean test(T value) {
            return !c.test(value);
        }
    }

    private static class And<T> implements Predicate<T> {
        final Predicate<? super T> c1;
        final Predicate<? super T> c2;

        And(Predicate<? super T> c1, Predicate<? super T> c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override
        public boolean test(T object) {
            return c1.test(object) && c2.test(object);
        }
    }

    private static class Or<T> implements Predicate<T> {
        final Predicate<? super T> c1;
        final Predicate<? super T> c2;

        Or(Predicate<? super T> c1, Predicate<? super T> c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override
        public boolean test(T object) {
            return c1.test(object) || c2.test(object);
        }
    }

    private static class SoftRefCache<T> implements Predicate<T> {
        private final HashMap<Integer, Pair<SoftReference<T>, Boolean>> myCache = new HashMap<>();
        private final Predicate<T> myCondition;

        public SoftRefCache(Predicate<T> condition) {
            myCondition = condition;
        }

        @Override
        public final boolean test(T object) {
            final int key = object.hashCode();
            final Pair<SoftReference<T>, Boolean> entry = myCache.get(key);
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
