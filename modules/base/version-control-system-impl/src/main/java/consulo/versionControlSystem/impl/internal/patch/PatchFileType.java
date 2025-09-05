/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.patch;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * @author yole
 * @since 2006-11-17
 */
public class PatchFileType implements FileType {
  public static final PatchFileType INSTANCE = new PatchFileType();

  @Override
  @Nonnull
  public String getId() {
    return "PATCH";
  }

  @Override
  @Nonnull
  public LocalizeValue getDescription() {
    return VcsLocalize.patchFileTypeDescription();
  }

  @Override
  @Nonnull
  public String getDefaultExtension() {
    return "patch";
  }

  @Override
  @Nonnull
  public Image getIcon() {
    return PlatformIconGroup.filetypesPatch();
  }

  public static boolean isPatchFile(@Nullable VirtualFile vFile) {
    return vFile != null && vFile.getFileType() == PatchFileType.INSTANCE;
  }

  public static boolean isPatchFile(@Nonnull File file) {
    return isPatchFile(VirtualFileUtil.findFileByIoFile(file, true));
  }
}
