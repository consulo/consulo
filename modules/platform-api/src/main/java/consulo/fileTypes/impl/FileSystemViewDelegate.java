/*
 * Copyright 2013-2016 consulo.io
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
package consulo.fileTypes.impl;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.awt.shell.ShellFolder;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 03-Sep-15
 */
public class FileSystemViewDelegate {
  private static Method ourShellFolderMethod;

  static {
    try {
      ourShellFolderMethod = FileSystemView.class.getDeclaredMethod("getShellFolder", File.class);
      ourShellFolderMethod.setAccessible(true);
    }
    catch (NoSuchMethodException ignored) {
    }
  }

  @Nullable
  public static Icon getIcon(@NotNull File file) {
    if (ourShellFolderMethod == null) {
      return getDefaultIcon(file);
    }

    try {
      Object object = ourShellFolderMethod.invoke(FileSystemView.getFileSystemView(), file);
      if (!(object instanceof ShellFolder)) {
        return getDefaultIcon(file);
      }

      if (SystemInfo.isWindows) {
        if(!JBUI.isHiDPI()) {
          return getIconFromShellFolder(file, (ShellFolder)object);
        }

        // on HiDPI monitors, ShellFolder return 32x32 only icons, and cut base icon
        // it ignore scale, for 2.5 scale return icon with 32x32 (and cut only 25% of icon, not resize)
        // that why - return default icon
      }
      else {
        return getIconFromShellFolder(file, (ShellFolder)object);
      }
    }
    catch (IllegalAccessException | InvocationTargetException ignored) {
    }
    return getDefaultIcon(file);
  }

  private static Icon getIconFromShellFolder(File file, ShellFolder sf) {
    Image icon = sf.getIcon(JBUI.isHiDPI());
    if (icon != null) {
      return new ImageIcon(icon, sf.getFolderType());
    }
    return getDefaultIcon(file);
  }

  private static Icon getDefaultIcon(@NotNull File f) {
    return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
  }
}
