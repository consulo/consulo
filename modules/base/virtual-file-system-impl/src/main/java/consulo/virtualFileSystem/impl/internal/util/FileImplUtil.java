/*
 * Copyright 2013-2025 consulo.io
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
package consulo.virtualFileSystem.impl.internal.util;

import consulo.logging.Logger;
import consulo.util.collection.HashingStrategy;
import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.internal.FileSystemUtil;
import consulo.virtualFileSystem.util.FilePathHashingStrategy;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 2025-06-20
 */
public class FileImplUtil {
    private static final Logger LOG = Logger.getInstance(FileImplUtil.class);

    public static final HashingStrategy<CharSequence> PATH_CHAR_SEQUENCE_HASHING_STRATEGY = FilePathHashingStrategy.createForCharSequence();

    public static boolean canWrite(@Nonnull String path) {
        FileAttributes attributes = FileSystemUtil.getAttributes(path);
        return attributes != null && attributes.isWritable();
    }

    public static void setReadOnlyAttribute(@Nonnull String path, boolean readOnlyFlag) {
        boolean writableFlag = !readOnlyFlag;
        if (!new File(path).setWritable(writableFlag, false) && canWrite(path) != writableFlag) {
            LOG.warn("Can't set writable attribute of '" + path + "' to '" + readOnlyFlag + "'");
        }
    }

    @Nonnull
    public static byte[] loadBytes(@Nonnull InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FileUtil.copy(stream, buffer);
        return buffer.toByteArray();
    }
}
