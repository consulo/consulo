/*
 * Copyright 2013-2016 consulo.io
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
package com.intellij.util.containers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author VISTALL
 * @since 11-Sep-16
 * <p>
 * Port com/intellij/util/containers/util.kt
 */
public class UtilKt {
  public static <T> boolean isEmpty(@Nullable Stream<T> stream) {
    return stream == null || !stream.findAny().isPresent();
  }

  public static <T> Stream<T> notNullize(@Nullable Stream<T> stream) {
    return stream == null ? Stream.<T>empty() : stream;
  }

  @SafeVarargs
  public static <T> Stream<T> concat(Stream<T>... args) {
    return Stream.of(args).reduce(Stream.empty(), Stream::concat);
  }

  @SafeVarargs
  @Nonnull
  public static <T> Stream<T> stream(T... args) {
    return args == null ? Stream.<T>empty() : Arrays.stream(args);
  }

  @Nullable
  public static <T> T getIfSingle(Stream<T> stream) {
    if (stream == null) {
      return null;
    }
    return stream.limit(2).map(Optional::ofNullable).reduce(Optional.empty(), (a, b) -> a.isPresent() ^ b.isPresent() ? b : Optional.empty()).orElse(null);
  }
}
