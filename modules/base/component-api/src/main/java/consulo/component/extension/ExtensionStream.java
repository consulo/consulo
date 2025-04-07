/*
 * Copyright 2013-2025 consulo.io
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
package consulo.component.extension;

import consulo.component.internal.EmptyExtensionStream;
import consulo.component.internal.ExtensionStreamImpl;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author UNV
 * @since 2025-04-07
 */
public sealed interface ExtensionStream<T> permits EmptyExtensionStream, ExtensionStreamImpl {
    <R> ExtensionStream<R> map(Function<? super T, ? extends R> mapper);

    <R> ExtensionStream<R> mapNonnull(Function<? super T, ? extends R> mapper);

    ExtensionStream<T> filter(Predicate<? super T> predicate);

    ExtensionStream<T> distinct();

    ExtensionStream<T> peek(Consumer<? super T> action);

    boolean anyMatch(Predicate<? super T> predicate);

    void forEach(Consumer<? super T> action);

    Optional<T> findFirst();

    List<T> toList();

    static <T> ExtensionStream<T> of(Stream<T> stream) {
        return new ExtensionStreamImpl<>(stream);
    }

    @SuppressWarnings("unchecked")
    static <T> ExtensionStream<T> empty() {
        return EmptyExtensionStream.INSTANCE;
    }
}
