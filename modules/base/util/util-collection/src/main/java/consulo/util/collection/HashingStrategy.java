// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.collection;

import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;


import java.util.Objects;

public interface HashingStrategy<T> {
    final class CanonicalHashingStrategy<T> implements HashingStrategy<T> {
        static final HashingStrategy<?> INSTANCE = new CanonicalHashingStrategy<>();

        @Override
        public int hashCode(@Nullable T value) {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(@Nullable T o1, @Nullable T o2) {
            return Objects.equals(o1, o2);
        }
    }

    final class IdentityHashingStrategy<T> implements HashingStrategy<T> {
        static final HashingStrategy<?> INSTANCE = new IdentityHashingStrategy<>();

        @Override
        public int hashCode(@Nullable T value) {
            return System.identityHashCode(value);
        }

        @Override
        public boolean equals(@Nullable T o1, @Nullable T o2) {
            return o1 == o2;
        }
    }

    final class CaseInsensitiveStringHashingStrategy implements HashingStrategy<String> {
        public static final HashingStrategy<String> INSTANCE = new CaseInsensitiveStringHashingStrategy();

        @Override
        public int hashCode(@Nullable String s) {
            return s == null ? 0 : StringUtil.stringHashCodeInsensitive(s);
        }

        @Override
        @SuppressWarnings("StringEquality")
        public boolean equals(@Nullable String s1, @Nullable String s2) {
            return s1 == s2 || s1 != null && s1.equalsIgnoreCase(s2);
        }
    }

    default int hashCode(@Nullable T object) {
        return Objects.hashCode(object);
    }

    default boolean equals(@Nullable T o1, @Nullable T o2) {
        return Objects.equals(o1, o2);
    }

    @SuppressWarnings("unchecked")
    static <T> HashingStrategy<T> canonical() {
        return (HashingStrategy<T>)CanonicalHashingStrategy.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    static <T> HashingStrategy<T> identity() {
        return (HashingStrategy<T>)IdentityHashingStrategy.INSTANCE;
    }

    static HashingStrategy<String> caseInsensitive() {
        return CaseInsensitiveStringHashingStrategy.INSTANCE;
    }
}