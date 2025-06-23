/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.virtualFileSystem.fileType;

import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author valentin
 * @since 2004-01-29
 */
public abstract class FakeFileType implements FileTypeIdentifiableByVirtualFile {

  @Override
  @Nonnull
  public String getDefaultExtension() {
    return "fakeExtension";
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return Image.empty(Image.DEFAULT_ICON_SIZE);
  }

  @Override
  public boolean isBinary() {
    return true;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }
}