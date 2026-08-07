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
package consulo.ui.clipboard;

import consulo.disposer.Disposable;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * System clipboard of a single {@link consulo.ui.UIAccess}.
 * <p>
 * Every system call answers a future, which completes exceptionally with
 * {@link ClipboardAccessException} when the backend refuses.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public interface Clipboard {
    boolean isSupported(ClipboardFeature feature);

    CompletableFuture<DataTransfer> getContents();

    CompletableFuture<Set<DataTransferType<?>>> getAvailableTypes();

    CompletableFuture<Void> setContents(DataTransfer transfer);

    CompletableFuture<Void> clear();

    /**
     * The half of the payload which never left this process.
     */
    DataTransfer getLocalContents();

    /**
     * The listener is told the previous and the current payload - a subscriber which decorates what was
     * copied needs the old one to undecorate it.
     */
    Disposable addContentListener(BiConsumer<DataTransfer, DataTransfer> listener);

    default CompletableFuture<@Nullable String> getText() {
        return getContents().<@Nullable String>thenApply(transfer -> transfer.get(DataTransferType.TEXT));
    }

    default CompletableFuture<Void> setText(String text) {
        return setContents(DataTransfer.of(text));
    }
}
