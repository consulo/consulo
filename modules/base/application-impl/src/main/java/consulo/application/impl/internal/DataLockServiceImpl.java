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
package consulo.application.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.DataLockService;
import consulo.application.WriteAction;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2025-07-16
 */
@Singleton
@ServiceImpl
public class DataLockServiceImpl implements DataLockService {
    @RequiredUIAccess
    @Nonnull
    @Override
    public <T> CompletableFuture<T> modalWrite(@Nonnull LocalizeValue modalText, @Nonnull Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        WriteAction.runAndWait(() -> {
            try {
                future.complete(callable.call());
            }
            catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
