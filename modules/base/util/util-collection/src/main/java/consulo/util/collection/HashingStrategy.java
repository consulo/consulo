// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.util.collection;

import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import java.util.Objects;

public interface HashingStrategy<T> {
  final class CanonicalHashingStrategy<T> implements HashingStrategy<T> {
    static final HashingStrategy<?> INSTANCE = new CanonicalHashingStrategy<>();

    @Override
    public int hashCode(T value) {
      return Objects.hashCode(value);
    }

    @Override
    public boolean equals(T o1, T o2) {
      return Objects.equals(o1, o2);
    }
  }

  final class IdentityHashingStrategy<T> implements HashingStrategy<T> {
    static final HashingStrategy<?> INSTANCE = new IdentityHashingStrategy<>();

    @Override
    public int hashCode(T value) {
      return System.identityHashCode(value);
    }

    @Override
    public boolean equals(T o1, T o2) {
      return o1 == o2;
    }
  }

  final class CaseInsensitiveStringHashingStrategy implements HashingStrategy<String> {
    public static final HashingStrategy<String> INSTANCE = new CaseInsensitiveStringHashingStrategy();

    @Override
    public int hashCode(String s) {
      return StringUtil.stringHashCodeInsensitive(s);
    }

    @Override
    public boolean equals(String s1, String s2) {
      assert s1 != null;
      return s1.equalsIgnoreCase(s2);
    }
  }

  default int hashCode(T object) {
    return Objects.hashCode(object);
  }

  default boolean equals(T o1, T o2) {
    return Objects.equals(o1, o2);
  }

  @Nonnull
  static <T> HashingStrategy<T> canonical() {
    //noinspection unchecked
    return (HashingStrategy<T>)CanonicalHashingStrategy.INSTANCE;
  }

  @Nonnull
  static <T> HashingStrategy<T> identity() {
    //noinspection unchecked
    return (HashingStrategy<T>)IdentityHashingStrategy.INSTANCE;
  }

  @Nonnull
  static HashingStrategy<String> caseInsensitive() {
    return CaseInsensitiveStringHashingStrategy.INSTANCE;
  }
}