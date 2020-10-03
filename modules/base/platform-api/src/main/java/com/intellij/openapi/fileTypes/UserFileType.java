// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileTypes;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.NotWorkingIconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

public abstract class UserFileType<T extends UserFileType> implements FileType, Cloneable {
  @Nonnull
  private String myName = "";
  private String myDescription = "";

  private Image myIcon;
  private String myIconPath;

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

  @Override
  @Nonnull
  public String getName() {
    return myName;
  }

  @Override
  @Nonnull
  public String getDescription() {
    return myDescription;
  }

  public void setName(@Nonnull String name) {
    myName = name;
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  @Override
  @Nonnull
  public String getDefaultExtension() {
    return "";
  }

  @Override
  public Image getIcon() {
    Image icon = myIcon;
    if (icon == null) {
      if (myIconPath != null) {
        icon = (Image)NotWorkingIconLoader.getIcon(myIconPath);
        myIcon = icon;
      }

      if (icon == null) {
        icon = PlatformIconGroup.fileTypesCustom();
      }
    }
    return icon;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public String getCharset(@Nonnull VirtualFile file, @Nonnull final byte[] content) {
    return null;
  }

  public void copyFrom(@Nonnull UserFileType newType) {
    myName = newType.getName();
    myDescription = newType.getDescription();
  }

  public void setIcon(@Nonnull Image icon) {
    myIcon = icon;
  }

  public void setIconPath(@Nonnull String value) {
    myIconPath = value;
  }

  @Override
  public String toString() {
    return myName;
  }
}
