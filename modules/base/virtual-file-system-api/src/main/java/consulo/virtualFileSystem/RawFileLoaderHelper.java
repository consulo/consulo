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

import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import consulo.virtualFileSystem.light.TextLightVirtualFileBase;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 16-Apr-22
 */
public class RawFileLoaderHelper {
  private static final Key<Boolean> OUR_NO_SIZE_LIMIT_KEY = Key.create("no.size.limit");

  public static boolean isTooLargeForIntelligence(@Nonnull VirtualFile vFile) {
    if (!checkFileSizeLimit(vFile)) return false;
    return fileSizeIsGreaterThan(vFile, RawFileLoader.getInstance().getMaxIntellisenseFileSize());
  }

  public static boolean isTooLargeForContentLoading(@Nonnull VirtualFile vFile) {
    return fileSizeIsGreaterThan(vFile, RawFileLoader.getInstance().getFileLengthToCacheThreshold());
  }

  private static boolean checkFileSizeLimit(@Nonnull VirtualFile vFile) {
    if (Boolean.TRUE.equals(vFile.getCopyableUserData(OUR_NO_SIZE_LIMIT_KEY))) {
      return false;
    }
    if (vFile instanceof LightVirtualFileBase) {
      VirtualFile original = ((LightVirtualFileBase)vFile).getOriginalFile();
      if (original != null) return checkFileSizeLimit(original);
    }
    return true;
  }

  public static void doNotCheckFileSizeLimit(@Nonnull VirtualFile vFile) {
    vFile.putCopyableUserData(OUR_NO_SIZE_LIMIT_KEY, Boolean.TRUE);
  }

  public static boolean isTooLargeForIntelligence(@Nonnull VirtualFile vFile, final long contentSize) {
    if (!checkFileSizeLimit(vFile)) return false;
    return contentSize > RawFileLoader.getInstance().getMaxIntellisenseFileSize();
  }

  public static boolean isTooLargeForContentLoading(@Nonnull VirtualFile vFile, final long contentSize) {
    return contentSize > RawFileLoader.getInstance().getFileLengthToCacheThreshold();
  }

  public static boolean fileSizeIsGreaterThan(@Nonnull VirtualFile vFile, final long maxBytes) {
    if (vFile instanceof TextLightVirtualFileBase) {
      // This is optimization in order to avoid conversion of [large] file contents to bytes
      final int lengthInChars = ((TextLightVirtualFileBase)vFile).getContent().length();
      if (lengthInChars < maxBytes / 2) return false;
      if (lengthInChars > maxBytes) return true;
    }

    return vFile.getLength() > maxBytes;
  }
}
