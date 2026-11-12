/*
 * Copyright 2013-2024 consulo.io
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
package consulo.component.store.impl.internal.storage.nio;

import consulo.component.store.internal.ReadOnlyModificationException;
import consulo.component.store.impl.internal.storage.StorageUtil;
import consulo.platform.LineSeparator;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2024-08-10
 */
public class PathStorageUtil {
    /**
     * True for a path of the local file system, false for one of a remote platform.
     */
    public static boolean isLocal(Path path) {
        return path.getFileSystem() == FileSystems.getDefault();
    }

    /**
     * Like {@link java.io.File#lastModified()} - 0 when the file does not exist or cannot be read.
     */
    public static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        }
        catch (IOException e) {
            return 0;
        }
    }

    /**
     * Like {@link java.io.File#length()} - 0 when the file does not exist or cannot be read.
     */
    public static long length(Path path) {
        try {
            return Files.size(path);
        }
        catch (IOException e) {
            return 0;
        }
    }

    public static void writeFile(Path file, byte[] content, @Nullable LineSeparator lineSeparatorIfPrependXmlProlog) throws IOException {
        try {

            try (OutputStream out = Files.newOutputStream(file)) {
                if (lineSeparatorIfPrependXmlProlog != null) {
                    out.write(StorageUtil.XML_PROLOG);
                    out.write(lineSeparatorIfPrependXmlProlog.getSeparatorBytes());
                }
                out.write(content);
            }
        }
        catch (AccessDeniedException e) {
            throw new ReadOnlyModificationException(file);
        }
    }
}
