// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.UISettings;
import consulo.fileEditor.UniqueVFilePathBuilder;
import consulo.fileEditor.EditorTabTitleProvider;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.io.FileUtilRt;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.io.File;

/**
 * @author yole
 */
@ExtensionImpl
public class UniqueNameEditorTabTitleProvider implements EditorTabTitleProvider {
  @Override
  public String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
    UISettings uiSettings = UISettings.getInstanceOrNull();
    if (uiSettings == null || !uiSettings.getShowDirectoryForNonUniqueFilenames() || DumbService.isDumb(project)) {
      return null;
    }

    // Even though this is a 'tab title provider' it is used also when tabs are not shown, namely for building IDE frame title.
    String uniqueName = uiSettings.getEditorTabPlacement() == UISettings.PLACEMENT_EDITOR_TAB_NONE
                        ? UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(project, file)
                        : UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePathWithinOpenedFileEditors(project, file);
    uniqueName = getEditorTabText(uniqueName, File.separator, uiSettings.getHideKnownExtensionInTabs());
    return uniqueName.equals(file.getName()) ? null : uniqueName;
  }

  public static String getEditorTabText(String result, String separator, boolean hideKnownExtensionInTabs) {
    if (hideKnownExtensionInTabs) {
      String withoutExtension = FileUtilRt.getNameWithoutExtension(result);
      if (StringUtil.isNotEmpty(withoutExtension) && !withoutExtension.endsWith(separator)) {
        return withoutExtension;
      }
    }
    return result;
  }
}
