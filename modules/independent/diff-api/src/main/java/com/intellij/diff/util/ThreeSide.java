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
package com.intellij.diff.util;

import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public enum ThreeSide {
  LEFT(0),
  BASE(1),
  RIGHT(2);

  private final int myIndex;

  ThreeSide(int index) {
    myIndex = index;
  }

  @Nonnull
  public static ThreeSide fromIndex(int index) {
    if (index == 0) return LEFT;
    if (index == 1) return BASE;
    if (index == 2) return RIGHT;
    throw new IndexOutOfBoundsException("index: " + index);
  }

  public int getIndex() {
    return myIndex;
  }

  //
  // Helpers
  //

  @Nullable
  @Contract("!null, !null, !null -> !null; null, null, null -> null")
  public <T> T select(@javax.annotation.Nullable T left, @Nullable T base, @Nullable T right) {
    if (myIndex == 0) return left;
    if (myIndex == 1) return base;
    if (myIndex == 2) return right;
    //noinspection Contract
    throw new IllegalStateException();
  }

  @Nonnull
  public <T> T selectNotNull(@Nonnull T left, @Nonnull T base, @Nonnull T right) {
    if (myIndex == 0) return left;
    if (myIndex == 1) return base;
    if (myIndex == 2) return right;
    throw new IllegalStateException();
  }

  public int select(@Nonnull int[] array) {
    assert array.length == 3;
    return array[myIndex];
  }

  public <T> T select(@Nonnull T[] array) {
    assert array.length == 3;
    return array[myIndex];
  }

  @Nonnull
  public <T> T selectNotNull(@Nonnull T[] array) {
    assert array.length == 3;
    return array[myIndex];
  }

  public <T> T select(@Nonnull List<T> list) {
    assert list.size() == 3;
    return list.get(myIndex);
  }

  @Nonnull
  public <T> T selectNotNull(@Nonnull List<T> list) {
    assert list.size() == 3;
    return list.get(myIndex);
  }

  @Nullable
  public static <T> ThreeSide fromValue(@Nonnull List<? extends T> list, @Nullable T value) {
    assert list.size() == 3;
    int index = list.indexOf(value);
    return index != -1 ? fromIndex(index) : null;
  }

  @Nonnull
  public static <T> List<T> map(@Nonnull Function<ThreeSide, T> function) {
    return ContainerUtil.list(
            function.fun(LEFT),
            function.fun(BASE),
            function.fun(RIGHT)
    );
  }
}
