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
package consulo.application.concurrent.coroutine;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-02-02
 */
public final class DisposableCoroutineScope {
    public static void launchAsync(CoroutineContext context,
                                   Disposable parentDisposable,
                                   Supplier<Coroutine<?, ?>> supplier) {
        CoroutineScope aScope = new CoroutineScope(context);

        Disposer.register(parentDisposable, aScope::cancel);

        Coroutine<?, ?> coroutine = supplier.get();

        coroutine.runAsync(aScope, null);
    }
}