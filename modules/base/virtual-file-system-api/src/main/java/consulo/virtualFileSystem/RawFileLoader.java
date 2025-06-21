/*
 * Copyright 2013-2022 consulo.io
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
package consulo.virtualFileSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.util.io.FileTooBigException;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @see RawFileLoaderHelper
 * @since 15-Feb-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface RawFileLoader {
    @Nonnull
    static RawFileLoader getInstance() {
        return Application.get().getInstance(RawFileLoader.class);
    }

    default byte[] loadFileBytes(@Nonnull Path path) throws IOException, FileTooBigException {
        return loadFileBytes(path.toFile());
    }

    @Nonnull
    byte[] loadFileBytes(@Nonnull File file) throws IOException, FileTooBigException;

    @Nonnull
    default String loadFileText(@Nonnull File file, @Nonnull Charset charset) throws IOException, FileTooBigException {
        return new String(loadFileBytes(file), charset);
    }

    default boolean isTooLarge(long length) {
        return isLargeForContentLoading(length);
    }

    default boolean isLargeForContentLoading(long length) {
        return length >= getFileLengthToCacheThreshold();
    }

    int getMaxIntellisenseFileSize();

    int getFileLengthToCacheThreshold();

    default int getLargeFilePreviewSize() {
        return getFileLengthToCacheThreshold();
    }

    int getUserContentLoadLimit();
}
