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
import io.qt.core.QUrl;
import io.qt.gui.QClipboard;
import io.qt.widgets.QApplication;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtClipboardImpl extends BaseClipboard {
    private static final String RTF_MIME = "text/rtf";

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

            QMimeData mimeData = clipboard.mimeData();
            if (mimeData == null) {
                return builder.build();
            }

            if (mimeData.hasText()) {
                builder.put(DataTransferType.TEXT, mimeData.text());
            }
            if (mimeData.hasHtml()) {
                builder.put(DataTransferType.HTML, mimeData.html());
            }
            if (mimeData.hasFormat(RTF_MIME)) {
                builder.put(DataTransferType.RTF, mimeData.data(RTF_MIME).toString());
            }
            if (mimeData.hasUrls()) {
                List<File> files = new ArrayList<>();
                for (QUrl url : mimeData.urls()) {
                    if (url.isLocalFile()) {
                        files.add(new File(url.toLocalFile()));
                    }
                }

                if (!files.isEmpty()) {
                    builder.put(DataTransferType.FILE_LIST, files);
                }
            }

            return builder.build();
        });
    }

    @Override
    protected CompletableFuture<Set<DataTransferType<?>>> readNativeTypes() {
        return onClipboard(clipboard -> {
            Set<DataTransferType<?>> types = new LinkedHashSet<>();

            QMimeData mimeData = clipboard.mimeData();
            if (mimeData == null) {
                return types;
            }

            for (String format : mimeData.formats()) {
                if (format.startsWith(DataTransferType.TEXT.getId())) {
                    types.add(DataTransferType.TEXT);
                }
                else if (format.startsWith(DataTransferType.HTML.getId())) {
                    types.add(DataTransferType.HTML);
                }
                else if (format.startsWith(RTF_MIME) || format.startsWith("application/rtf")) {
                    types.add(DataTransferType.RTF);
                }
                else if (format.startsWith(DataTransferType.FILE_LIST.getId())) {
                    types.add(DataTransferType.FILE_LIST);
                }
            }

            return types;
        });
    }

    @Override
    protected CompletableFuture<Void> writeNative(DataTransfer transfer) {
        return onClipboard(clipboard -> {
            QMimeData mimeData = new QMimeData();
            boolean any = false;

            String text = transfer.get(DataTransferType.TEXT);
            if (text != null) {
                mimeData.setText(text);
                any = true;
            }

            String html = transfer.get(DataTransferType.HTML);
            if (html != null) {
                mimeData.setHtml(html);
                any = true;
            }

            String rtf = transfer.get(DataTransferType.RTF);
            if (rtf != null) {
                mimeData.setData(RTF_MIME, rtf.getBytes(StandardCharsets.UTF_8));
                any = true;
            }

            List<File> files = transfer.get(DataTransferType.FILE_LIST);
            if (files != null && !files.isEmpty()) {
                List<QUrl> urls = new ArrayList<>(files.size());
                for (File file : files) {
                    urls.add(QUrl.fromLocalFile(file.getAbsolutePath()));
                }
                mimeData.setUrls(urls);
                any = true;
            }

            if (any) {
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
