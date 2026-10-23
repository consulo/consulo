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
package consulo.component.persist;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * JSON binding of {@link com.dslplatform.json.CompiledJson} states, given to {@link StateSplitterEx} by a storage that writes
 * split states as JSON. Each state is bound by the converters of its own class loader.
 * <p>
 * A tree is a JSON value as plain objects: {@code Map<String, Object>}, {@code List<Object>}, {@code String}, {@code Number},
 * {@code Boolean} or {@code null}.
 *
 * @author VISTALL
 * @since 2026-10-23
 */
public interface StateJsonSerializer {
    boolean isJsonCapable(Class<?> stateClass);

    /**
     * Values equal to those of a fresh state are left out.
     */
    byte[] serialize(Object state) throws IOException;

    <T> @Nullable T deserialize(Class<T> stateClass, byte[] content) throws IOException;

    @Nullable Object readTree(byte[] content) throws IOException;

    byte[] writeTree(@Nullable Object tree) throws IOException;
}
