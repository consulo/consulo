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
package consulo.virtualFileSystem.impl.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.util.io.FileTooBigException;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.RawFileLoader;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 15-Feb-22
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.PRODUCTION)
public class RawFileLoaderImpl implements RawFileLoader {
    private static final int KILOBYTE = 1024;
    private static final int MEGABYTE = KILOBYTE * KILOBYTE;

    private static final int DEFAULT_INTELLISENSE_LIMIT = 2500 * KILOBYTE;

    private final int myLargeForContentLoading = Math.max(20 * MEGABYTE, Math.max(getUserFileSizeLimitImpl(), getUserContentLoadLimitImpl()));
    private final int myLargeFilePreviewSize = Math.min(getLargeFilePreviewSizeImpl(), myLargeForContentLoading);

    private final int myUserFileSizeLimit = getUserContentLoadLimit();

    private int myMaxIntellisenseFileSize = Math.min(RawFileLoaderImpl.getUserFileSizeLimitImpl(), (int) myLargeForContentLoading);

    private static int getUserFileSizeLimitImpl() {
        return parseKilobyteProperty("idea.max.intellisense.filesize", DEFAULT_INTELLISENSE_LIMIT);
    }

    private static int getUserContentLoadLimitImpl() {
        return parseKilobyteProperty("idea.max.content.load.filesize", 20 * MEGABYTE);
    }

    private static int getLargeFilePreviewSizeImpl() {
        return parseKilobyteProperty("idea.max.content.load.large.preview.size", DEFAULT_INTELLISENSE_LIMIT);
    }

    @Nonnull
    @Override
    public byte[] loadFileBytes(@Nonnull File file) throws IOException, FileTooBigException {
        byte[] bytes;
        try (InputStream stream = new FileInputStream(file)) {
            long len = file.length();
            if (len < 0) {
                throw new IOException("File length reported negative, probably doesn't exist");
            }

            if (isTooLarge(len)) {
                throw new FileTooBigException("Attempt to load '" + file + "' in memory buffer, file length is " + len + " bytes.");
            }

            bytes = FileUtil.loadBytes(stream, (int) len);
        }
        return bytes;
    }

    @Override
    public boolean isLargeForContentLoading(long length) {
        return length >= myLargeForContentLoading;
    }

    @Override
    public int getMaxIntellisenseFileSize() {
        return myMaxIntellisenseFileSize;
    }

    @Override
    public int getFileLengthToCacheThreshold() {
        return myLargeForContentLoading;
    }

    @Override
    public int getLargeFilePreviewSize() {
        return myLargeFilePreviewSize;
    }

    @Override
    public int getUserContentLoadLimit() {
        return myUserFileSizeLimit;
    }

    private static int parseKilobyteProperty(String key, int defaultValue) {
        try {
            long i = Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue / KILOBYTE)));
            if (i < 0) {
                return Integer.MAX_VALUE;
            }
            return (int) Math.min(i * KILOBYTE, Integer.MAX_VALUE);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
