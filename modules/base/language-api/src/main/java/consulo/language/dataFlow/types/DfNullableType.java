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

/**
 * A type of values that may be absent ({@code null}, {@code nil}, {@code None}...): references, optionals, pointers.
 * The language layer implements it with its reference types (Java: {@code DfReferenceType}) and decides which
 * constants are absent; {@link DfaNullability#fromDfType(DfType)} reads the nullability of any {@link DfType}.
 *
 * @see DfNotNullableType
 */
public interface DfNullableType extends DfType {
  /**
   * @return nullability of this type
   */
  DfaNullability getNullability();

  /**
   * @return this type without nullability knowledge, or simply this type if it's a constant
   */
  DfNullableType dropNullability();
}
