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
package consulo.ui.impl.clipboard;

import consulo.disposer.Disposable;
import consulo.ui.clipboard.*;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public abstract class BaseClipboard implements Clipboard {
    private final List<BiConsumer<DataTransfer, DataTransfer>> myListeners = new CopyOnWriteArrayList<>();

    private volatile DataTransfer myTransfer = DataTransfer.EMPTY;
    private volatile DataTransfer myLocalTransfer = DataTransfer.EMPTY;
    private volatile @Nullable String myLocalOwnerText;

    protected abstract CompletableFuture<DataTransfer> readNative();

    protected abstract CompletableFuture<Set<DataTransferType<?>>> readNativeTypes();

    protected abstract CompletableFuture<Void> writeNative(DataTransfer transfer);

    protected abstract CompletableFuture<Void> clearNative();

    @Override
    public CompletableFuture<DataTransfer> getContents() {
        return readNative().thenApply(this::mergeLocal);
    }

    @Override
    public CompletableFuture<Set<DataTransferType<?>>> getAvailableTypes() {
        return readNativeTypes().thenApply(nativeTypes -> {
            Set<DataTransferType<?>> types = new LinkedHashSet<>(nativeTypes);
            types.addAll(myLocalTransfer.getTypes());
            return types;
        });
    }

    @Override
    public CompletableFuture<Void> setContents(DataTransfer transfer) {
        DataTransfer old = myTransfer;
        myTransfer = transfer;
        myLocalTransfer = transfer.filter(type -> !type.isNative());
        myLocalOwnerText = transfer.get(DataTransferType.TEXT);

        return writeNative(transfer.filter(DataTransferType::isNative))
            .thenRun(() -> fireContentChanged(old, transfer));
    }

    @Override
    public CompletableFuture<Void> clear() {
        DataTransfer old = myTransfer;
        dropLocal();

        return clearNative().thenRun(() -> fireContentChanged(old, DataTransfer.EMPTY));
    }

    @Override
    public DataTransfer getLocalContents() {
        return myLocalTransfer;
    }

    @Override
    public Disposable addContentListener(BiConsumer<DataTransfer, DataTransfer> listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }

    /**
     * Another application took the clipboard - what we held is gone and what replaced it is unknown
     * until somebody reads.
     */
    protected void fireForeignChange() {
        DataTransfer old = myTransfer;
        dropLocal();
        fireContentChanged(old, DataTransfer.EMPTY);
    }

    private void fireContentChanged(DataTransfer oldTransfer, DataTransfer newTransfer) {
        for (BiConsumer<DataTransfer, DataTransfer> listener : myListeners) {
            listener.accept(oldTransfer, newTransfer);
        }
    }

    private DataTransfer mergeLocal(DataTransfer nativeTransfer) {
        DataTransfer local = myLocalTransfer;
        if (local.isEmpty()) {
            return nativeTransfer;
        }

        // another application took the clipboard, our objects no longer describe what will be pasted
        if (!Objects.equals(myLocalOwnerText, nativeTransfer.get(DataTransferType.TEXT))) {
            dropLocal();
            return nativeTransfer;
        }

        return DataTransfer.builder().putAll(nativeTransfer).putAll(local).build();
    }

    private void dropLocal() {
        myTransfer = DataTransfer.EMPTY;
        myLocalTransfer = DataTransfer.EMPTY;
        myLocalOwnerText = null;
    }
}
