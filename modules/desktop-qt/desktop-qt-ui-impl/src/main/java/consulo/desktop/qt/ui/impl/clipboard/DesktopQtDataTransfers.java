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

import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import io.qt.core.QMimeData;
import io.qt.core.QUrl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class DesktopQtDataTransfers {
    private static final String RTF_MIME = "text/rtf";

    public static DataTransfer fromMimeData(QMimeData mimeData) {
        if (mimeData instanceof DesktopQtTransferMimeData transferMimeData) {
            return transferMimeData.getTransfer();
        }
        return readNative(mimeData);
    }

    public static QMimeData toMimeData(DataTransfer transfer) {
        DesktopQtTransferMimeData mimeData = new DesktopQtTransferMimeData(transfer);
        writeNative(mimeData, transfer);
        return mimeData;
    }

    public static DataTransfer readNative(QMimeData mimeData) {
        DataTransfer.Builder builder = DataTransfer.builder();
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
    }

    public static Set<DataTransferType<?>> readNativeTypes(QMimeData mimeData) {
        Set<DataTransferType<?>> types = new LinkedHashSet<>();
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
    }

    public static boolean writeNative(QMimeData mimeData, DataTransfer transfer) {
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

        return any;
    }

    private DesktopQtDataTransfers() {
    }
}
