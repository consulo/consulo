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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2024-08-10
 */
public class PathStorageUtil {
    public static void writeFile(@Nonnull Path file, @Nonnull byte[] content, @Nullable LineSeparator lineSeparatorIfPrependXmlProlog) throws IOException {
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
            throw new ReadOnlyModificationException(file.toFile());
        }
    }
}
