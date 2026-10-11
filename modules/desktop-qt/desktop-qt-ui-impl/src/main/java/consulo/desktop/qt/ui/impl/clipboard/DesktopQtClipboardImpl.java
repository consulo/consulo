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
package consulo.desktop.qt.ui.impl.clipboard;

import consulo.desktop.qt.ui.impl.DesktopQtUIAccess;
import consulo.ui.clipboard.ClipboardAccessException;
import consulo.ui.clipboard.ClipboardFeature;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.impl.clipboard.BaseClipboard;
import io.qt.core.QMimeData;
import io.qt.gui.QClipboard;
import io.qt.widgets.QApplication;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtClipboardImpl extends BaseClipboard {
    private static final Set<ClipboardFeature> FEATURES = EnumSet.of(
        ClipboardFeature.AVAILABLE_TYPES,
        ClipboardFeature.UNRESTRICTED_READ
    );

    @Override
    public boolean isSupported(ClipboardFeature feature) {
        return FEATURES.contains(feature);
    }

    @Override
    protected CompletableFuture<DataTransfer> readNative() {
        return onClipboard(clipboard -> {
            QMimeData mimeData = clipboard.mimeData();
            return mimeData == null ? DataTransfer.EMPTY : DesktopQtDataTransfers.readNative(mimeData);
        });
    }

    @Override
    protected CompletableFuture<Set<DataTransferType<?>>> readNativeTypes() {
        return onClipboard(clipboard -> {
            QMimeData mimeData = clipboard.mimeData();
            return mimeData == null ? Set.<DataTransferType<?>>of() : DesktopQtDataTransfers.readNativeTypes(mimeData);
        });
    }

    @Override
    protected CompletableFuture<Void> writeNative(DataTransfer transfer) {
        return onClipboard(clipboard -> {
            QMimeData mimeData = new QMimeData();
            if (DesktopQtDataTransfers.writeNative(mimeData, transfer)) {
                clipboard.setMimeData(mimeData);
            }
            else {
                mimeData.dispose();
            }

            return null;
        });
    }

    @Override
    protected CompletableFuture<Void> clearNative() {
        return onClipboard(clipboard -> {
            clipboard.clear();
            return null;
        });
    }

    private <T> CompletableFuture<T> onClipboard(Function<QClipboard, T> function) {
        CompletableFuture<T> future = new CompletableFuture<>();

        DesktopQtUIAccess.INSTANCE.give(() -> {
            try {
                future.complete(function.apply(QApplication.clipboard()));
            }
            catch (Throwable e) {
                future.completeExceptionally(new ClipboardAccessException("Clipboard call failed", e));
            }
        });

        return future;
    }
}
