// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.dataFlow.types;

import consulo.language.localize.LanguageLocalize;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Represents whether a value may be absent within DFA. "Absent" is the language's empty reference:
 * {@code null} in Java and C#, {@code nil} in Swift and Go, {@code None} in Python, {@code null}/{@code undefined} in TypeScript.
 * The lattice is the same for every language; which values are absent is decided by the types of the language
 * (see {@link DfNullableType}).
 * <p>
 * Moved from the Java data flow layer of JetBrains (where it is tied to {@code null} and the Java nullability annotations);
 * the conversions to the annotation nullability of a language stay in the language layer.
 */
public enum DfaNullability {
  /**
   * Means: exactly absent (null)
   */
  NULL("Null", LanguageLocalize::dataflowNullabilityNull),
  NULLABLE("Nullable", LanguageLocalize::dataflowNullabilityNullable),
  NOT_NULL("Not-null", LanguageLocalize::dataflowNullabilityNonNull),
  UNKNOWN("Unknown", LocalizeValue::empty),
  /**
   * Means: non-stable variable declared as Nullable was checked for nullity and flushed afterwards (e.g. by unknown method call),
   * so we are unsure about its nullability anymore.
   */
  FLUSHED("Flushed", LocalizeValue::empty);

  private final String myInternalName;
  private final Supplier<LocalizeValue> myPresentationalName;

  DfaNullability(String internalName, Supplier<LocalizeValue> presentationalName) {
    myInternalName = internalName;
    myPresentationalName = presentationalName;
  }

  public String getInternalName() {
    return myInternalName;
  }

  public String getPresentationName() {
    return myPresentationalName.get().get();
  }

  /**
   * @return true if the value may be absent: {@link #NULL} or {@link #NULLABLE}
   */
  public boolean mayBeAbsent() {
    return this == NULL || this == NULLABLE;
  }

  public DfaNullability unite(DfaNullability other) {
    if (this == other) {
      return this;
    }
    if (this == NULL || other == NULL ||
        this == NULLABLE || other == NULLABLE) {
      return NULLABLE;
    }
    if (this == FLUSHED || other == FLUSHED) {
      return FLUSHED;
    }
    return UNKNOWN;
  }

  public @Nullable DfaNullability intersect(DfaNullability right) {
    if (this == NOT_NULL) {
      return right == NULL ? null : NOT_NULL;
    }
    if (right == NOT_NULL) {
      return this == NULL ? null : NOT_NULL;
    }
    if (this == UNKNOWN) return right;
    if (right == UNKNOWN) return this;
    if (this == FLUSHED && right.mayBeAbsent() ||
        right == FLUSHED && mayBeAbsent()) {
      return NULLABLE;
    }
    return equals(right) ? this : null;
  }

  /**
   * @return nullability of the values of the type: {@link #NOT_NULL} for {@link DfType#FAIL} and value types
   * ({@link DfNotNullableType}), the nullability of a {@link DfNullableType}, {@link #UNKNOWN} otherwise
   */
  public static DfaNullability fromDfType(DfType type) {
    return type == DfType.FAIL || type instanceof DfNotNullableType ? NOT_NULL :
           type instanceof DfNullableType nullableType ? nullableType.getNullability() : UNKNOWN;
  }
}
