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

import consulo.ui.clipboard.ClipboardFeature;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A clipboard which never leaves this process - for headless runs and for a web session with no
 * browser attached.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public class MemoryClipboard extends BaseClipboard {
    private volatile DataTransfer myTransfer = DataTransfer.EMPTY;

    @Override
    public boolean isSupported(ClipboardFeature feature) {
        return false;
    }

    @Override
    protected CompletableFuture<DataTransfer> readNative() {
        return CompletableFuture.completedFuture(myTransfer);
    }

    @Override
    protected CompletableFuture<Set<DataTransferType<?>>> readNativeTypes() {
        return CompletableFuture.completedFuture(myTransfer.getTypes());
    }

    @Override
    protected CompletableFuture<Void> writeNative(DataTransfer transfer) {
        myTransfer = transfer;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> clearNative() {
        myTransfer = DataTransfer.EMPTY;
        return CompletableFuture.completedFuture(null);
    }
}
