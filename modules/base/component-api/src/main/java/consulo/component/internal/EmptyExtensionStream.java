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
package consulo.component.internal;

import consulo.component.extension.ExtensionStream;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author UNV
 * @since 2025-04-07
 */
public final class EmptyExtensionStream<T> implements ExtensionStream<T> {
    public static final EmptyExtensionStream INSTANCE = new EmptyExtensionStream();

    @Override
    @SuppressWarnings("unchecked")
    public <R> ExtensionStream<R> map(Function<? super T, ? extends R> mapper) {
        return INSTANCE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> ExtensionStream<R> mapNonnull(Function<? super T, ? extends R> mapper) {
        return INSTANCE;
    }

    @Override
    public ExtensionStream<T> filter(Predicate<? super T> predicate) {
        return this;
    }

    @Override
    public ExtensionStream<T> distinct() {
        return this;
    }

    @Override
    public ExtensionStream<T> peek(Consumer<? super T> action) {
        return this;
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return false;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
    }

    @Override
    public Optional<T> findFirst() {
        return Optional.empty();
    }

    @Override
    public List<T> toList() {
        return List.of();
    }

    private EmptyExtensionStream() {
    }
}
