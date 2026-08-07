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

import java.io.File;
import java.util.List;

/**
 * A typed key for one representation of a clipboard payload.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public final class DataTransferType<T> {
    public static final DataTransferType<String> TEXT = ofMime("text/plain");
    public static final DataTransferType<String> HTML = ofMime("text/html");
    public static final DataTransferType<String> RTF = ofMime("text/rtf");
    public static final DataTransferType<List<File>> FILE_LIST = ofMime("text/uri-list");
    public static final DataTransferType<byte[]> IMAGE = ofMime("image/png");

    /**
     * A created type never leaves this process.
     */
    public static <T> DataTransferType<T> create(String id) {
        return new DataTransferType<>(id, false);
    }

    private static <T> DataTransferType<T> ofMime(String mimeType) {
        return new DataTransferType<>(mimeType, true);
    }

    private final String myId;
    private final boolean myNative;

    private DataTransferType(String id, boolean isNative) {
        myId = id;
        myNative = isNative;
    }

    public String getId() {
        return myId;
    }

    public boolean isNative() {
        return myNative;
    }

    @Override
    public String toString() {
        return (myNative ? "native:" : "local:") + myId;
    }
}
