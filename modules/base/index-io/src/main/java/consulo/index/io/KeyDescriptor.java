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
package consulo.index.io;

import consulo.index.io.data.DataExternalizer;
import consulo.util.collection.HashingStrategy;

import java.util.Objects;

/**
 * @author max
 */
public interface KeyDescriptor<T> extends HashingStrategy<T>, DataExternalizer<T> {
    @Override
    default boolean equals(T o1, T o2) {
        return Objects.equals(o1, o2);
    }

    @Override
    default int hashCode(T object) {
        return object.hashCode();
    }
}
