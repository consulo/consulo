// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs.newvfs;

import com.intellij.openapi.fileEditor.impl.EditorTabPresentationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.awt.*;

import static com.intellij.openapi.util.text.StringUtil.escapeMnemonics;
import static com.intellij.openapi.util.text.StringUtil.firstLast;

/**
 * @author gregsh
 */
public class VfsPresentationUtil {

  @Nonnull
  public static String getPresentableNameForAction(@Nonnull Project project, @Nonnull VirtualFile file) {
    return escapeMnemonics(firstLast(getPresentableNameForUI(project, file), 20));
  }

  @Nonnull
  public static String getPresentableNameForUI(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getEditorTabTitle(project, file, null);
  }

  @Nonnull
  public static String getUniquePresentableNameForUI(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getUniqueEditorTabTitle(project, file, null);
  }

  @Nullable
  public static Color getFileTabBackgroundColor(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getEditorTabBackgroundColor(project, file, null);
  }

  @Nullable
  public static Color getFileBackgroundColor(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getFileBackgroundColor(project, file);
  }
}
