/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.psi.util;

import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface CachedValueProvider<T> {
  @Nullable
  Result<T> compute();

  class Result<T> {
    private final T myValue;
    private final Object[] myDependencyItems;

    public Result(@Nullable T value, Object... dependencyItems) {
      myValue = value;
      myDependencyItems = dependencyItems;
    }

    public T getValue() {
      return myValue;
    }

    public Object[] getDependencyItems() {
      return myDependencyItems;
    }

    public static <T> Result<T> createSingleDependency(@Nullable T value, Object dependency) {
      return create(value, dependency);
    }

    public static <T> Result<T> create(@Nullable T value, Object... dependencies) {
      return new Result<T>(value, dependencies);
    }

    public static <T> Result<T> create(@Nullable T value, Collection<?> dependencies) {
      return new Result<T>(value, ArrayUtil.toObjectArray(dependencies));
    }

    @Deprecated
    public void setLockValue(final boolean lockValue) {
    }
  }
}
