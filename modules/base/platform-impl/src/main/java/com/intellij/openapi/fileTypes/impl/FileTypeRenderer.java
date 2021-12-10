/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.fileTypes.impl;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class FileTypeRenderer extends ColoredListCellRenderer<FileType> {
  public interface FileTypeListProvider {
    Iterable<FileType> getCurrentFileTypeList();
  }

  private final FileTypeListProvider myFileTypeListProvider;

  public FileTypeRenderer() {
    this(new DefaultFileTypeListProvider());
  }

  public FileTypeRenderer(@Nonnull FileTypeListProvider fileTypeListProvider) {
    myFileTypeListProvider = fileTypeListProvider;
  }

  @Override
  protected void customizeCellRenderer(@Nonnull JList<? extends FileType> list, FileType type, int index, boolean selected, boolean hasFocus) {
    setEnabled(list.isEnabled());
    
    setIcon(type.getIcon());

    String description = type.getDescription().get();
    String trimmedDescription = StringUtil.capitalizeWords(description.replaceAll("(?i)\\s*file(?:s)?$", ""), true);
    if (isDuplicated(description)) {
      append(trimmedDescription + " (" + type.getId() + ")");

    }
    else {
      append(trimmedDescription);
    }
  }

  private boolean isDuplicated(final String description) {
    boolean found = false;

    for (FileType type : myFileTypeListProvider.getCurrentFileTypeList()) {
      if (description.equals(type.getDescription())) {
        if (!found) {
          found = true;
        }
        else {
          return true;
        }
      }
    }
    return false;
  }

  private static class DefaultFileTypeListProvider implements FileTypeListProvider {
    private final List<FileType> myFileTypes;

    public DefaultFileTypeListProvider() {
      myFileTypes = Arrays.asList(FileTypeManager.getInstance().getRegisteredFileTypes());
    }

    @Override
    public Iterable<FileType> getCurrentFileTypeList() {
      return myFileTypes;
    }
  }
}
