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
package consulo.ui.internal;

import consulo.ui.UIAccess;
import consulo.ui.UIAccessScheduler;
import consulo.ui.clipboard.Clipboard;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-09-23
 */
public class InvalidUIAccess implements UIAccess {
    public static final InvalidUIAccess INSTANCE = new InvalidUIAccess();

    private UnsupportedOperationException t() {
        return new UnsupportedOperationException("UI is invalid");
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public Clipboard getClipboard() {
        throw t();
    }

    @Override
    public void give(Runnable runnable) {
        throw t();
    }

    @Override
    public <T> CompletableFuture<T> giveAsync(Supplier<T> supplier) {
        return CompletableFuture.failedFuture(t());
    }

    @Override
    public UIAccessScheduler getScheduler() {
        throw t();
    }

    @Nullable
    @Override
    public <T> T getUserData(Key<T> key) {
        throw t();
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        throw t();
    }
}
