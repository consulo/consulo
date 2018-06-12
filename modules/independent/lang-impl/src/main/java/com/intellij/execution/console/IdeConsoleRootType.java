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
package com.intellij.execution.console;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author gregsh
 */
public class IdeConsoleRootType extends ConsoleRootType {
  IdeConsoleRootType() {
    super("ide", "IDE Scripting");
  }

  @Nonnull
  public static IdeConsoleRootType getInstance() {
    return findByClass(IdeConsoleRootType.class);
  }

  @Nullable
  @Override
  public Image substituteIcon(@Nonnull Project project, @Nonnull VirtualFile file) {
    FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(file.getName());
    if (fileType == UnknownFileType.INSTANCE || fileType == PlainTextFileType.INSTANCE) {
      return AllIcons.Debugger.ToolConsole;
    }
    //Icon icon = TargetAWT.to(fileType.getIcon());
    //Icon subscript = ((ScalableIcon)AllIcons.Debugger.ToolConsole).scale(.5f);
    //LayeredIcon icons = new LayeredIcon(2);
    //icons.setIcon(icon, 0);
    //icons.setIcon(subscript, 1, 8, 8);
    //return icons;
    Image icon = fileType.getIcon();
    assert icon != null;
    return ImageEffects.transparent(icon, .8f);
  }

  @Override
  public void fileOpened(@Nonnull VirtualFile file, @Nonnull FileEditorManager source) {
    RunIdeConsoleAction.configureConsole(file, source);
  }

}
