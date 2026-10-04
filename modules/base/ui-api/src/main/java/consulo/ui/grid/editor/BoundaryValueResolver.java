// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid.editor;

import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Map;
import java.util.function.Function;

public interface BoundaryValueResolver {
    BoundaryValueResolver ALWAYS_NULL = new BoundaryValueResolver() {
        @Override
        public Object createJdbcNegativeInfinityValue() {
            throw new UnsupportedOperationException("Should never happens");
        }

        @Override
        public Object createJdbcPositiveInfinityValue() {
            throw new UnsupportedOperationException("Should never happens");
        }

        @Override
        public java.util.Date getPresentablePositiveInfinity() {
            throw new UnsupportedOperationException("Should never happens");
        }

        @Override
        public java.util.Date getPresentableNegativeInfinity() {
            throw new UnsupportedOperationException("Should never happens");
        }

        @Override
        public boolean isPositiveInfinity(Object object) {
            return false;
        }

        @Override
        public boolean isNegativeInfinity(Object object) {
            return false;
        }

        @Override
        public @Nullable String getPositiveInfinityString() {
            return null;
        }

        @Override
        public @Nullable String getNegativeInfinityString() {
            return null;
        }

        @Override
        public @Nullable Class<?> getObjectClass() {
            return null;
        }
    };

    java.util.Date getPresentablePositiveInfinity();

    java.util.Date getPresentableNegativeInfinity();

    Object createJdbcPositiveInfinityValue();

    Object createJdbcNegativeInfinityValue();

    boolean isPositiveInfinity(Object object);

    boolean isNegativeInfinity(Object object);

    @Nullable
    String getPositiveInfinityString();

    @Nullable
    String getNegativeInfinityString();

    @Nullable
    Class<?> getObjectClass();

    default @Nullable String resolve(Object value) {
        Class<?> objectClass = getObjectClass();
        return objectClass != null && objectClass.isInstance(value) ? getInfinityString(value) : null;
    }

    default @Nullable String getInfinityString(@Nullable Object value) {
        return value != null && isPositiveInfinity(value) ? getPositiveInfinityString() :
            value != null && isNegativeInfinity(value) ? getNegativeInfinityString() :
                null;
    }

    default java.util.Date bound(Object object) {
        return isPositiveInfinity(object) ? getPresentablePositiveInfinity() :
            isNegativeInfinity(object) ? getPresentableNegativeInfinity() :
                getLegacyDate(object);
    }

    default @Nullable Object createFromInfinityString(String value) {
        return StringUtil.equalsIgnoreWhitespaces(value, getPositiveInfinityString()) ? createJdbcPositiveInfinityValue() :
            StringUtil.equalsIgnoreWhitespaces(value, getNegativeInfinityString()) ? createJdbcNegativeInfinityValue() :
                null;
    }

    // throws for a class which is not in the rules
    @SuppressWarnings("NullAway")
    default java.util.Date getLegacyDate(Object object) {
        return Rules.MAP.get(object.getClass()).apply(object);
    }

    final class Rules {
        static final Map<Class<?>, Function<Object, java.util.Date>> MAP =
            Map.of(
                java.util.Date.class, o -> (java.util.Date) o,
                java.sql.Date.class, o -> (java.util.Date) o,
                Timestamp.class, o -> (java.util.Date) o,
                Time.class, o -> (java.util.Date) o);
    }
}
