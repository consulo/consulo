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
import consulo.component.util.PluginExceptionUtil;
import consulo.logging.Logger;
import consulo.util.collection.UnmodifiableHashMap;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author UNV
 * @since 2025-04-07
 */
public final class ExtensionStreamImpl<T> implements ExtensionStream<T> {
    @Nonnull
    private final Stream<T> mySubStream;

    public ExtensionStreamImpl(@Nonnull Stream<T> subStream) {
        mySubStream = subStream;
    }

    @Override
    public <R> ExtensionStream<R> map(Function<? super T, ? extends R> mapper) {
        return ExtensionStream.of(mySubStream.flatMap(value -> {
            try {
                return Stream.of(mapper.apply(value));
            }
            catch (Throwable e) {
                checkException(e, value);
            }
            return Stream.empty();
        }));
    }

    @Override
    public ExtensionStream<T> filter(Predicate<? super T> predicate) {
        return ExtensionStream.of(mySubStream.flatMap(value -> {
            try {
                boolean matches = predicate.test(value);
                if (matches) {
                    return Stream.of(value);
                }
            }
            catch (Throwable e) {
                checkException(e, value);
            }
            return Stream.empty();
        }));
    }

    @Override
    public ExtensionStream<T> distinct() {
        SimpleReference<UnmodifiableHashMap<T, Boolean>> map = SimpleReference.create(UnmodifiableHashMap.empty());
        mySubStream.forEach(value -> {
            UnmodifiableHashMap<T, Boolean> m = map.get();
            try {
                if (!m.containsKey(value)) {
                    map.set(m.with(value, Boolean.TRUE));
                }
            }
            catch (Throwable e) {
                checkException(e, value);
            }
        });
        return ExtensionStream.of(map.get().keySet().stream());
    }

    @Override
    public ExtensionStream<T> peek(Consumer<? super T> action) {
        return ExtensionStream.of(mySubStream.peek(value -> {
            try {
                action.accept(value);
            }
            catch (Throwable e) {
                checkException(e, value);
            }
        }));
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return mySubStream.anyMatch(value -> {
            try {
                return predicate.test(value);
            }
            catch (Throwable e) {
                checkException(e, value);
                return false;
            }
        });
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        mySubStream.forEach(value -> {
            try {
                action.accept(value);
            }
            catch (Throwable e) {
                checkException(e, value);
            }
        });
    }

    @Override
    public Optional<T> findFirst() {
        return mySubStream.findFirst();
    }

    @Override
    public List<T> toList() {
        return mySubStream.collect(Collectors.toList());
    }

    private void checkException(Throwable e, Object value) {
        if (e instanceof ControlFlowException) {
            throw ControlFlowException.rethrow(e);
        }
        Logger logger = Logger.getInstance(ExtensionStreamImpl.class);
        PluginExceptionUtil.logPluginError(logger, e.getMessage(), e, value != null ? value.getClass() : Void.TYPE);
    }
}
