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
package consulo.ui.ex;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.ClipboardAccessException;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.clipboard.DataTransfer;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Kill ring, copy history and the cut marker on top of {@link consulo.ui.clipboard.Clipboard}.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CopyPasteManager {
    ColorValue CUT_COLOR = new RGBColor(160, 160, 160);

    static CopyPasteManager getInstance() {
        return Application.get().getInstance(CopyPasteManager.class);
    }

    @RequiredUIAccess
    CompletableFuture<Void> setContents(DataTransfer transfer);

    @RequiredUIAccess
    CompletableFuture<DataTransfer> getContents();

    /**
     * The half of the payload which never left this process, read synchronously.
     */
    @RequiredUIAccess
    DataTransfer getLocalContents();

    /**
     * A registration outlives the {@link consulo.ui.UIAccess} it was made on - reopening a project
     * builds a new one and the listener has to keep firing - so it ends only when {@code parent} is
     * disposed.
     */
    @RequiredUIAccess
    void addContentListener(BiConsumer<DataTransfer, DataTransfer> listener, Disposable parent);

    @RequiredUIAccess
    void setCutElements(Object @Nullable [] elements);

    boolean isCutElement(@Nullable Object element);

    /**
     * Subsequent adjacent copies merge into one region. Every non adjacent change breaks the merge, and
     * some actions - an undo for instance - have to break it by hand.
     */
    void stopKillRings();

    @RequiredUIAccess
    default CompletableFuture<Void> setText(String text) {
        return setContents(DataTransfer.of(text));
    }

    /**
     * One representation of what would be pasted. The local payload answers at once when this process wrote
     * last, otherwise the system clipboard is read - which is a round trip on a frontend where the clipboard
     * lives in the browser, so the answer is a future rather than a value.
     * <p>
     * Completes with {@code null} when the clipboard holds nothing of that type, and exceptionally with
     * {@link ClipboardAccessException} when the frontend was refused.
     */
    @RequiredUIAccess
    default <T> CompletableFuture<@Nullable T> getContentsAsync(DataTransferType<T> type) {
        T local = getLocalContents().get(type);
        if (local != null) {
            return CompletableFuture.completedFuture(local);
        }

        return getContents().thenApply(transfer -> transfer.get(type));
    }
}
