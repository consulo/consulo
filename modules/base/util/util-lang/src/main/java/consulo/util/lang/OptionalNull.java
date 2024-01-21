/*
 * Copyright 2013-2023 consulo.io
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
package consulo.util.lang;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2023-12-20
 */
public final class OptionalNull<V> implements Supplier<V> {
  @SuppressWarnings("unchecked")
  private static final OptionalNull EMPTY = new OptionalNull(null, false);

  @SuppressWarnings("unchecked")
  @Nonnull
  public static <K> OptionalNull<K> empty() {
    return EMPTY;
  }

  @Nonnull
  public static <K> OptionalNull<K> ofNullable(@Nullable K value) {
    return new OptionalNull<>(value, true);
  }

  private final V myValue;
  private final boolean myValueSet;

  private OptionalNull(V value, boolean valueSet) {
    myValue = value;
    myValueSet = valueSet;
  }

  public boolean isValueSet() {
    return myValueSet;
  }

  public boolean hasValue() {
    return myValueSet && myValue != null;
  }

  public V anyOr(@Nullable V otherValue) {
      if (isValueSet()) {
        return myValue;
      }

      return otherValue;
  }

  public V anyNull() {
    return anyOr(null);
  }

  @Override
  public V get() {
    if (!myValueSet) {
      throw new IllegalArgumentException("Value not set");
    }
    return myValue;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("OptionalNull{");
    sb.append("myValue=").append(myValue);
    sb.append(", myValueSet=").append(myValueSet);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OptionalNull<?> that = (OptionalNull<?>)o;
    return myValueSet == that.myValueSet &&
      Objects.equals(myValue, that.myValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myValue, myValueSet);
  }
}
