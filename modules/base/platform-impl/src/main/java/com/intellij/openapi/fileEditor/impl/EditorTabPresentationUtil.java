// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileEditor.impl;

import com.intellij.openapi.fileEditor.UniqueVFilePathBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileEditor.impl.EditorWindow;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;

public class EditorTabPresentationUtil {
  @Nonnull
  public static String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file, @Nullable EditorWindow editorWindow) {
    for (EditorTabTitleProvider provider : DumbService.getDumbAwareExtensions(project, EditorTabTitleProvider.EP_NAME)) {
      String result = provider.getEditorTabTitle(project, file, editorWindow);
      if (StringUtil.isNotEmpty(result)) {
        return result;
      }
    }

    return file.getPresentableName();
  }

  @Nonnull
  public static String getUniqueEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file, @Nullable EditorWindow editorWindow) {
    String name = getEditorTabTitle(project, file, editorWindow);
    if (name.equals(file.getPresentableName())) {
      return UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(project, file);
    }
    return name;
  }

  @Nullable
  public static Color getEditorTabBackgroundColor(@Nonnull Project project, @Nonnull VirtualFile file, @Nullable EditorWindow editorWindow) {
    for (EditorTabColorProvider provider : DumbService.getDumbAwareExtensions(project, EditorTabColorProvider.EP_NAME)) {
      Color result = provider.getEditorTabColor(project, file, editorWindow);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Nullable
  public static Color getFileBackgroundColor(@Nonnull Project project, @Nonnull VirtualFile file) {
    for (EditorTabColorProvider provider : DumbService.getDumbAwareExtensions(project, EditorTabColorProvider.EP_NAME)) {
      Color result = provider.getProjectViewColor(project, file);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
}
