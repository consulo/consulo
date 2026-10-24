// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.http.json;

import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

public interface JsonDataDeserializer {
    /**
     * Parses a value of the given type from the given {@code bodyReader}.
     * <p>
     * The reader is not closed by this function. It should be managed by the caller.
     */
    <T> @Nullable T fromJson(Reader bodyReader, Class<T> clazz);

    /**
     * Parses a value of type T = L&lt;A, B&gt; from the given {@code bodyReader}.
     * Type T is given by {@code clazz}, whereas A and B are given through {@code classArgs}.
     * <p>
     * The reader is not closed by this function. It should be managed by the caller.
     */
    <T> @Nullable T fromJson(Reader bodyReader, Class<T> clazz, Class<?>... classArgs);

    /**
     * Read a value of the given type from the given {@code stream}.
     * <p>
     * The stream should not be closed by this function. It should be managed by the caller.
     */
    <T> @Nullable T readJson(InputStream stream, Charset charset, Class<T> clazz);

    /**
     * Read a value of type T = L&lt;A, B&gt; from the given {@code stream}.
     * Type T is given by {@code clazz}, whereas A and B are given through {@code classArgs}.
     * <p>
     * The stream should not be closed by this function. It should be managed by the caller.
     */
    <T> @Nullable T readJson(InputStream stream, Charset charset, Class<T> clazz, Class<?>... classArgs);
}
