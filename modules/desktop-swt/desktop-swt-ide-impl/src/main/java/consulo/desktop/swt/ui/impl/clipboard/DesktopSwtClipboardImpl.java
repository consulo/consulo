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
package consulo.desktop.swt.ui.impl.clipboard;

import consulo.desktop.swt.ui.impl.DesktopSwtUIAccess;
import consulo.ui.clipboard.ClipboardAccessException;
import consulo.ui.clipboard.ClipboardFeature;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.impl.clipboard.BaseClipboard;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Display;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public class DesktopSwtClipboardImpl extends BaseClipboard {
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
            DataTransfer.Builder builder = DataTransfer.builder();

            if (clipboard.getContents(TextTransfer.getInstance()) instanceof String text) {
                builder.put(DataTransferType.TEXT, text);
            }
            if (clipboard.getContents(HTMLTransfer.getInstance()) instanceof String html) {
                builder.put(DataTransferType.HTML, html);
            }
            if (clipboard.getContents(RTFTransfer.getInstance()) instanceof String rtf) {
                builder.put(DataTransferType.RTF, rtf);
            }
            if (clipboard.getContents(FileTransfer.getInstance()) instanceof String[] paths) {
                List<File> files = new ArrayList<>(paths.length);
                for (String path : paths) {
                    files.add(new File(path));
                }
                builder.put(DataTransferType.FILE_LIST, files);
            }
            return builder.build();
        });
    }

    @Override
    protected CompletableFuture<Set<DataTransferType<?>>> readNativeTypes() {
        return onClipboard(clipboard -> {
            Set<DataTransferType<?>> types = new LinkedHashSet<>();
            for (TransferData transferData : clipboard.getAvailableTypes()) {
                if (TextTransfer.getInstance().isSupportedType(transferData)) {
                    types.add(DataTransferType.TEXT);
                }
                else if (HTMLTransfer.getInstance().isSupportedType(transferData)) {
                    types.add(DataTransferType.HTML);
                }
                else if (RTFTransfer.getInstance().isSupportedType(transferData)) {
                    types.add(DataTransferType.RTF);
                }
                else if (FileTransfer.getInstance().isSupportedType(transferData)) {
                    types.add(DataTransferType.FILE_LIST);
                }
            }
            return types;
        });
    }

    @Override
    protected CompletableFuture<Void> writeNative(DataTransfer transfer) {
        return onClipboard(clipboard -> {
            List<Object> values = new ArrayList<>();
            List<Transfer> transfers = new ArrayList<>();

            String text = transfer.get(DataTransferType.TEXT);
            if (text != null) {
                values.add(text);
                transfers.add(TextTransfer.getInstance());
            }

            String html = transfer.get(DataTransferType.HTML);
            if (html != null) {
                values.add(html);
                transfers.add(HTMLTransfer.getInstance());
            }

            String rtf = transfer.get(DataTransferType.RTF);
            if (rtf != null) {
                values.add(rtf);
                transfers.add(RTFTransfer.getInstance());
            }

            List<File> files = transfer.get(DataTransferType.FILE_LIST);
            if (files != null) {
                String[] paths = new String[files.size()];
                for (int i = 0; i < files.size(); i++) {
                    paths[i] = files.get(i).getAbsolutePath();
                }
                values.add(paths);
                transfers.add(FileTransfer.getInstance());
            }

            if (!values.isEmpty()) {
                clipboard.setContents(values.toArray(), transfers.toArray(new Transfer[0]));
            }
            return null;
        });
    }

    @Override
    protected CompletableFuture<Void> clearNative() {
        return onClipboard(clipboard -> {
            clipboard.clearContents();
            return null;
        });
    }

    private <T> CompletableFuture<T> onClipboard(Function<Clipboard, T> function) {
        Display display = DesktopSwtUIAccess.INSTANCE.getDisplay();

        CompletableFuture<T> future = new CompletableFuture<>();
        display.asyncExec(() -> {
            Clipboard clipboard = new Clipboard(display);
            try {
                future.complete(function.apply(clipboard));
            }
            catch (Throwable e) {
                future.completeExceptionally(new ClipboardAccessException("Clipboard call failed", e));
            }
            finally {
                clipboard.dispose();
            }
        });
        return future;
    }
}
