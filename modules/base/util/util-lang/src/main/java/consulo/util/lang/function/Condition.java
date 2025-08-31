/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import org.jetbrains.annotations.NonNls;

import java.util.function.Predicate;

/**
 * Returns {@code true} or {@code false} for the given input object.
 * <p/>
 * See {@link Conditions} for chained conditions.
 *
 * @author dsl
 */
@Deprecated
@DeprecationInfo("Use Predicate")
@FunctionalInterface
public interface Condition<T> extends Predicate<T> {
    boolean value(T t);

    @Override
    default boolean test(T t) {
        return value(t);
    }

    Condition<Object> NOT_NULL = new Condition<>() {
        @Override
        public boolean value(Object object) {
            return object != null;
        }

        @NonNls
        @Override
        public String toString() {
            return "Condition.NOT_NULL";
        }
    };

    /**
     * @see Conditions#alwaysTrue()
     */
    Condition TRUE = new Condition() {
        @Override
        public boolean value(Object object) {
            return true;
        }

        @NonNls
        @Override
        public String toString() {
            return "Condition.TRUE";
        }
    };
    /**
     * @see Conditions#alwaysFalse()
     */
    Condition FALSE = new Condition() {
        @Override
        public boolean value(Object object) {
            return false;
        }

        @NonNls
        @Override
        public String toString() {
            return "Condition.FALSE";
        }
    };
}
