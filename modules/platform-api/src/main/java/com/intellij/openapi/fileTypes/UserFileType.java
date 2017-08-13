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
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.util.pointers.Named;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class UserFileType <T extends UserFileType> implements FileType, Named, Cloneable {
  @NotNull private String myName = "";
  private String myDescription = "";
  private Icon myIcon = AllIcons.FileTypes.Custom;

  public abstract SettingsEditor<T> getEditor();

  @Override
  public UserFileType clone() {
    try {
      return (UserFileType)super.clone();
    }
    catch (CloneNotSupportedException e) {
      return null; //Can't be
    }
  }

  @NotNull
  @Override
  public String getId() {
    return myName;
  }

  @Override
  @NotNull
  public String getName() {
    return myName;
  }

  @Override
  @NotNull
  public String getDescription() {
    return myDescription;
  }

  public void setName(@NotNull String name) {
    myName = name;
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  @Override
  @NotNull
  public String getDefaultExtension() {
    return "";
  }

  @Override
  public Icon getIcon() {
    return myIcon;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public String getCharset(@NotNull VirtualFile file, final byte[] content) {
    return null;
  }

  public void copyFrom(UserFileType newType) {
    myName = newType.getName();
    myDescription = newType.getDescription();
  }

  public void setIcon(Icon icon) {
    myIcon = icon;
  }

  @Override
  public String toString() {
    return myName;
  }
}
