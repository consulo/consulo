/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.ide.highlighter;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by ggrnd0 on 28.09.14.
 */
public class XmlFileType implements FileType {
  public static final XmlFileType INSTANCE = new XmlFileType();
  public static final String DOT_DEFAULT_EXTENSION = ".xml";

  private static final NotNullLazyValue<Icon> ICON = new NotNullLazyValue<Icon>() {
    @NotNull
    @Override
    protected Icon compute() {
      return AllIcons.FileTypes.Xml;
    }
  };

  @NotNull
  @Override
  public String getName() {
    return "XML_FILE";
  }

  @NotNull
  @Override
  public String getDescription() {
    return IdeBundle.message("filetype.description.xml");
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "xml";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return ICON.getValue();
  }

  @Override
  public boolean isBinary() {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public String getCharset(@NotNull VirtualFile file, final byte[] content) {
    return null;
  }
}
