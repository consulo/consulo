// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor;

import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author gregsh
 */
public class VfsPresentationUtil {

  
  public static String getPresentableNameForAction(Project project, VirtualFile file) {
    return StringUtil.escapeMnemonics(StringUtil.firstLast(getPresentableNameForUI(project, file), 20));
  }

  
  public static String getPresentableNameForUI(Project project, VirtualFile file) {
    return EditorTabPresentationUtil.getEditorTabTitle(project, file);
  }

  
  public static String getUniquePresentableNameForUI(Project project, VirtualFile file) {
    return EditorTabPresentationUtil.getUniqueEditorTabTitle(project, file);
  }

  public static @Nullable ColorValue getFileTabBackgroundColor(Project project, VirtualFile file) {
    return EditorTabPresentationUtil.getEditorTabBackgroundColor(project, file, null);
  }

  public static @Nullable ColorValue getFileBackgroundColor(Project project, VirtualFile file) {
    return EditorTabPresentationUtil.getFileBackgroundColor(project, file);
  }
}
