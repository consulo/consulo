/*
 * Copyright 2013-2015 must-be.org
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
      if(!(object instanceof ShellFolder)) {
        return getDefaultIcon(file);
      }

      ShellFolder sf = (ShellFolder)object;
      Image icon = sf.getIcon(JBUI.scale(1f) > 1.5f);
      if(icon != null) {
        return new ImageIcon(icon, sf.getFolderType());
      }
    }
    catch (IllegalAccessException ignored) {
    }
    catch (InvocationTargetException ignored) {
    }
    return getDefaultIcon(file);
  }

  private static Icon getDefaultIcon(@NotNull File f) {
    return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
  }
}
