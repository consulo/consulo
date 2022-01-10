/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.ui.debugger.extensions;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * Created by IntelliJ IDEA.
 * User: alexander.chernikov
 * Date: Mar 23, 2010
 * Time: 7:01:01 PM
 */
public class UiScriptFileType implements FileType {
  private static UiScriptFileType myInstance;

  private UiScriptFileType() {
  }

  public static UiScriptFileType getInstance() {
    if (myInstance == null) {
      myInstance = new UiScriptFileType();
    }
    return myInstance;
  }

  @Override
  @Nonnull
  public String getId() {
    return "UI Script";
  }

  @Override
  @Nonnull
  public LocalizeValue getDescription() {
    return LocalizeValue.of("UI test scripts.");
  }

  public static final String myExtension = "ijs";

  @Override
  @Nonnull
  public String getDefaultExtension() {
    return myExtension;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return Image.empty(Image.DEFAULT_ICON_SIZE);
  }

  @Override
  public String getCharset(@Nonnull VirtualFile file, byte[] content) {
    return CharsetToolkit.UTF8;
  }
}
