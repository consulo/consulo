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

import com.intellij.diff.fragments.DiffFragment;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.openapi.util.Couple;
import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public enum Side {
  LEFT(0),
  RIGHT(1);

  private final int myIndex;

  Side(int index) {
    myIndex = index;
  }

  @Nonnull
  public static Side fromIndex(int index) {
    if (index == 0) return LEFT;
    if (index == 1) return RIGHT;
    throw new IndexOutOfBoundsException("index: " + index);
  }

  @Nonnull
  public static Side fromLeft(boolean isLeft) {
    return isLeft ? LEFT : RIGHT;
  }

  @Nonnull
  public static Side fromRight(boolean isRight) {
    return isRight ? RIGHT : LEFT;
  }

  public int getIndex() {
    return myIndex;
  }

  public boolean isLeft() {
    return myIndex == 0;
  }

  @Nonnull
  public Side other() {
    return isLeft() ? RIGHT : LEFT;
  }

  @Nonnull
  public Side other(boolean other) {
    return other ? other() : this;
  }

  //
  // Helpers
  //

  public int select(int left, int right) {
    return isLeft() ? left : right;
  }

  @Nullable
  @Contract("!null, !null -> !null; null, null -> null")
  public <T> T select(@Nullable T left, @Nullable T right) {
    return isLeft() ? left : right;
  }

  @Nonnull
  public <T> T selectNotNull(@Nonnull T left, @Nonnull T right) {
    return isLeft() ? left : right;
  }

  public boolean select(@Nonnull boolean[] array) {
    assert array.length == 2;
    return array[myIndex];
  }

  public int select(@Nonnull int[] array) {
    assert array.length == 2;
    return array[myIndex];
  }

  public <T> T select(@Nonnull T[] array) {
    assert array.length == 2;
    return array[myIndex];
  }

  @Nonnull
  public <T> T selectNotNull(@Nonnull T[] array) {
    assert array.length == 2;
    return array[myIndex];
  }

  public <T> T select(@Nonnull List<T> list) {
    assert list.size() == 2;
    return list.get(myIndex);
  }

  @Nonnull
  public <T> T selectNotNull(@Nonnull List<T> list) {
    assert list.size() == 2;
    return list.get(myIndex);
  }

  public <T> T select(@Nonnull Couple<T> region) {
    return isLeft() ? region.first : region.second;
  }

  @Nonnull
  public <T> T selectNotNull(@Nonnull Couple<T> region) {
    return isLeft() ? region.first : region.second;
  }

  @Nullable
  public static <T> Side fromValue(@Nonnull List<? extends T> list, @Nullable T value) {
    assert list.size() == 2;
    int index = list.indexOf(value);
    return index != -1 ? fromIndex(index) : null;
  }

  //
  // Fragments
  //

  public int getStartOffset(@Nonnull DiffFragment fragment) {
    return isLeft() ? fragment.getStartOffset1() : fragment.getStartOffset2();
  }

  public int getEndOffset(@Nonnull DiffFragment fragment) {
    return isLeft() ? fragment.getEndOffset1() : fragment.getEndOffset2();
  }

  public int getStartLine(@Nonnull LineFragment fragment) {
    return isLeft() ? fragment.getStartLine1() : fragment.getStartLine2();
  }

  public int getEndLine(@Nonnull LineFragment fragment) {
    return isLeft() ? fragment.getEndLine1() : fragment.getEndLine2();
  }
}
