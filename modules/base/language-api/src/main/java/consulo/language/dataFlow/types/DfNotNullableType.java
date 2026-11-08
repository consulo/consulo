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
 * A type of values that are never absent: primitive and value types (Java primitives, C# structs except {@code Nullable<T>},
 * Swift and Rust values). {@link DfaNullability#fromDfType(DfType)} reports {@link DfaNullability#NOT_NULL} for them.
 *
 * @see DfNullableType
 */
public interface DfNotNullableType extends DfType {
}
