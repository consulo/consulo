// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor;

import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author gregsh
 */
public class VfsPresentationUtil {

  @Nonnull
  public static String getPresentableNameForAction(@Nonnull Project project, @Nonnull VirtualFile file) {
    return StringUtil.escapeMnemonics(StringUtil.firstLast(getPresentableNameForUI(project, file), 20));
  }

  @Nonnull
  public static String getPresentableNameForUI(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getEditorTabTitle(project, file);
  }

  @Nonnull
  public static String getUniquePresentableNameForUI(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getUniqueEditorTabTitle(project, file);
  }

  @Nullable
  public static ColorValue getFileTabBackgroundColor(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getEditorTabBackgroundColor(project, file, null);
  }

  @Nullable
  public static ColorValue getFileBackgroundColor(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getFileBackgroundColor(project, file);
  }
}
