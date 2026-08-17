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
package consulo.desktop.awt.ui.impl.clipboard;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.ui.clipboard.ClipboardAccessException;
import consulo.ui.clipboard.ClipboardFeature;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.desktop.awt.internal.clipboard.DesktopAWTDataTransfers;
import consulo.ui.impl.clipboard.BaseClipboard;
import org.jspecify.annotations.Nullable;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public class DesktopAWTClipboardImpl extends BaseClipboard {
    private static final Logger LOG = Logger.getInstance(DesktopAWTClipboardImpl.class);

    private static final Set<ClipboardFeature> FEATURES = EnumSet.of(
        ClipboardFeature.AVAILABLE_TYPES,
        ClipboardFeature.CONTENT_LISTENER,
        ClipboardFeature.UNRESTRICTED_READ
    );

    // the owning application answers at its own pace, keep it off any ui thread
    private final Executor myExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Clipboard", 1);

    public DesktopAWTClipboardImpl() {
        Clipboard clipboard = getClipboard();
        if (clipboard != null) {
            clipboard.addFlavorListener(event -> fireForeignChange());
        }
    }

    @Override
    public boolean isSupported(ClipboardFeature feature) {
        return FEATURES.contains(feature);
    }

    @Override
    protected CompletableFuture<DataTransfer> readNative() {
        return onClipboard(clipboard -> {
            Transferable transferable = clipboard.getContents(null);
            return transferable == null ? DataTransfer.EMPTY : DesktopAWTDataTransfers.fromTransferable(transferable);
        });
    }

    @Override
    protected CompletableFuture<Set<DataTransferType<?>>> readNativeTypes() {
        return onClipboard(clipboard -> {
            Set<DataTransferType<?>> types = new LinkedHashSet<>();
            for (Map.Entry<DataTransferType<?>, DataFlavor> entry : DesktopAWTDataTransfers.flavors().entrySet()) {
                if (clipboard.isDataFlavorAvailable(entry.getValue())) {
                    types.add(entry.getKey());
                }
            }
            return types;
        });
    }

    @Override
    protected CompletableFuture<Void> writeNative(DataTransfer transfer) {
        return onClipboard(clipboard -> {
            clipboard.setContents(DesktopAWTDataTransfers.toTransferable(transfer), null);
            return null;
        });
    }

    @Override
    protected CompletableFuture<Void> clearNative() {
        return writeNative(DataTransfer.of(""));
    }

    private <T> CompletableFuture<T> onClipboard(Function<Clipboard, T> function) {
        Clipboard clipboard = getClipboard();
        if (clipboard == null) {
            return CompletableFuture.failedFuture(new ClipboardAccessException("No system clipboard"));
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        myExecutor.execute(() -> {
            try {
                future.complete(function.apply(clipboard));
            }
            catch (Throwable e) {
                future.completeExceptionally(new ClipboardAccessException("Clipboard call failed", e));
            }
        });
        return future;
    }

    private static @Nullable Clipboard getClipboard() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        catch (IllegalStateException e) {
            LOG.warn(e);
            return null;
        }
    }

}
