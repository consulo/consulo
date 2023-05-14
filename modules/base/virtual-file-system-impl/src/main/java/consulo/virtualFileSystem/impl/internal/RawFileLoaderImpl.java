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

import consulo.annotation.component.ServiceImpl;
import consulo.util.io.FileTooBigException;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.RawFileLoader;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 15-Feb-22
 */
@Singleton
@ServiceImpl
public class RawFileLoaderImpl implements RawFileLoader {
  private static final int KILOBYTE = 1024;

  private static final int DEFAULT_INTELLISENSE_LIMIT = 2500 * KILOBYTE;
  public static final int MEGABYTE = KILOBYTE * KILOBYTE;
  public static final int LARGE_FOR_CONTENT_LOADING = Math.max(20 * MEGABYTE, Math.max(getUserFileSizeLimit(), getUserContentLoadLimit()));
  public static final int LARGE_FILE_PREVIEW_SIZE = Math.min(getLargeFilePreviewSize(), LARGE_FOR_CONTENT_LOADING);

  /**
   * always  in range [0, PersistentFS.FILE_LENGTH_TO_CACHE_THRESHOLD]
   */
  private static int ourMaxIntellisenseFileSize = Math.min(RawFileLoaderImpl.getUserFileSizeLimit(), (int)LARGE_FOR_CONTENT_LOADING);

  @Nonnull
  @Override
  public byte[] loadFileBytes(@Nonnull File file) throws IOException, FileTooBigException {
    byte[] bytes;
    try (InputStream stream = new FileInputStream(file)) {
      final long len = file.length();
      if (len < 0) {
        throw new IOException("File length reported negative, probably doesn't exist");
      }

      if (isTooLarge(len)) {
        throw new FileTooBigException("Attempt to load '" + file + "' in memory buffer, file length is " + len + " bytes.");
      }

      bytes = FileUtil.loadBytes(stream, (int)len);
    }
    return bytes;
  }

  @Override
  public boolean isLargeForContentLoading(long length) {
    return length >= LARGE_FOR_CONTENT_LOADING;
  }

  @Override
  public int getMaxIntellisenseFileSize() {
    return ourMaxIntellisenseFileSize;
  }

  @Override
  public int getFileLengthToCacheThreshold() {
    return LARGE_FOR_CONTENT_LOADING;
  }

  private static int parseKilobyteProperty(String key, int defaultValue) {
    try {
      long i = Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue / KILOBYTE)));
      if (i < 0) return Integer.MAX_VALUE;
      return (int)Math.min(i * KILOBYTE, Integer.MAX_VALUE);
    }
    catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public static int getUserFileSizeLimit() {
    return parseKilobyteProperty("idea.max.intellisense.filesize", DEFAULT_INTELLISENSE_LIMIT);
  }

  public static int getUserContentLoadLimit() {
    return parseKilobyteProperty("idea.max.content.load.filesize", 20 * MEGABYTE);
  }

  private static int getLargeFilePreviewSize() {
    return parseKilobyteProperty("idea.max.content.load.large.preview.size", DEFAULT_INTELLISENSE_LIMIT);
  }
}
