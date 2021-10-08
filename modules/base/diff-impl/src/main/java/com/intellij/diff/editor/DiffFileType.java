/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.diff.editor;

import com.intellij.openapi.diff.DiffBundle;
import com.intellij.openapi.fileTypes.FileType;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23/09/2021
 */
public class DiffFileType implements FileType {
  public static final DiffFileType INSTANCE = new DiffFileType();
  
  private DiffFileType() {
  }

  @Nonnull
  @Override
  public String getId() {
    return "DIFF";
  }

  @Nonnull
  @Override
  public String getDescription() {
    return DiffBundle.message("filetype.diff.description");
  }

  @Nonnull
  @Override
  public String getDefaultExtension() {
    return "";
  }

  @Override
  public boolean isBinary() {
    return true;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return PlatformIconGroup.actionsDiff();
  }
}
