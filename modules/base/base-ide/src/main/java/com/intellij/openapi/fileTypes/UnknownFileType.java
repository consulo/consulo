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
package com.intellij.openapi.fileTypes;

import com.intellij.icons.AllIcons;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.FileTypeLocalize;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

public class UnknownFileType implements FileType {
  public static final FileType INSTANCE = new UnknownFileType();

  private UnknownFileType() {}

  @Override
  @Nonnull
  public String getId() {
    return "UNKNOWN";
  }

  @Override
  @Nonnull
  public LocalizeValue getDescription() {
    return FileTypeLocalize.unknownFileTypeDescription();
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.FileTypes.Unknown;
  }
}
