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
package consulo.review.impl.internal;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class ReviewUIUtil {
    private ReviewUIUtil() {
    }

    /**
     * Applies the current value now and every later one on the UI thread, until {@code parentDisposable} goes away.
     */
    @RequiredUIAccess
    public static <T extends @Nullable Object> void bind(Disposable parentDisposable, ObservableValue<T> value, @RequiredUIAccess Consumer<T> uiSetter) {
        UIAccess uiAccess = UIAccess.current();
        AtomicBoolean disposed = new AtomicBoolean();
        uiSetter.accept(value.get());
        Runnable unsubscribe = value.addListener(newValue -> uiAccess.give(() -> {
            if (!disposed.get()) {
                uiSetter.accept(newValue);
            }
        }));
        Disposer.register(parentDisposable, () -> {
            disposed.set(true);
            unsubscribe.run();
        });
    }

    /**
     * Applies both current values now and again whenever either changes, until {@code parentDisposable} goes away.
     */
    @RequiredUIAccess
    public static <A extends @Nullable Object, B extends @Nullable Object> void bind(Disposable parentDisposable,
                                   ObservableValue<A> first,
                                   ObservableValue<B> second,
                                   @RequiredUIAccess BiConsumer<A, B> uiSetter) {
        UIAccess uiAccess = UIAccess.current();
        AtomicBoolean disposed = new AtomicBoolean();
        Runnable update = () -> uiAccess.give(() -> {
            if (!disposed.get()) {
                uiSetter.accept(first.get(), second.get());
            }
        });
        uiSetter.accept(first.get(), second.get());
        Runnable firstUnsubscribe = first.addListener(value -> update.run());
        Runnable secondUnsubscribe = second.addListener(value -> update.run());
        Disposer.register(parentDisposable, () -> {
            disposed.set(true);
            firstUnsubscribe.run();
            secondUnsubscribe.run();
        });
    }
}
