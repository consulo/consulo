// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.uast.util;

import consulo.language.uast.internal.ClassSetImpl;
import consulo.language.uast.internal.ClassSetImpl1;
import consulo.language.uast.internal.ClassSetImpl2;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A set of classes with a fast "is subtype of any member" check.
 * <p>
 * The top-level factory functions of the original {@code ClassSet.kt} live here as static methods.
 */
public interface ClassSet<T> {
    /**
     * The empty set: contains nothing, converts to an empty list.
     */
    ClassSet<?> EMPTY = new ClassSet<Object>() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Class<? extends Object> element) {
            return false;
        }

        @Override
        public List<Class<? extends Object>> toList() {
            return List.of();
        }

        @Override
        public String toString() {
            return "EmptyClassSet";
        }
    };

    boolean isEmpty();

    boolean contains(Class<? extends T> element);

    List<Class<? extends T>> toList();

    static <T> boolean isInstanceOf(@Nullable T instance, ClassSet<T> classSet) {
        if (instance == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Class<? extends T> instanceClass = (Class<? extends T>) instance.getClass();
        return classSet.contains(instanceClass);
    }

    static <T> boolean hasClassOf(ClassSet<T> classSet, @Nullable T instance) {
        if (instance == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Class<? extends T> instanceClass = (Class<? extends T>) instance.getClass();
        return classSet.contains(instanceClass);
    }

    @SafeVarargs
    static <T> ClassSet<T> classSetOf(Class<? extends T>... classes) {
        if (classes.length == 0) {
            return emptyClassSet();
        }

        switch (classes.length) {
            case 0:
                return emptyClassSet();
            case 1:
                return new ClassSetImpl1<>(classes[0]);
            case 2:
                return new ClassSetImpl2<>(classes[0], classes[1]);
            default:
                return new ClassSetImpl<>(Arrays.asList(classes));
        }
    }

    @SafeVarargs
    static <T> ClassSet<T> classSetsUnion(ClassSet<T>... classSets) {
        List<Class<? extends T>> classes = new ArrayList<>();
        for (ClassSet<T> classSet : classSets) {
            classes.addAll(classSet.toList());
        }
        @SuppressWarnings("unchecked")
        Class<? extends T>[] array = classes.toArray(new Class[0]);
        return classSetOf(array);
    }

    @SuppressWarnings("unchecked")
    static <T> ClassSet<T> emptyClassSet() {
        return (ClassSet<T>) EMPTY;
    }
}
