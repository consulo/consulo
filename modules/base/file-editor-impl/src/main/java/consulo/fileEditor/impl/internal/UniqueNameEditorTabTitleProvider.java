// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.application.ui.UISettings;
import consulo.fileEditor.EditorTabTitleProvider;
import consulo.fileEditor.UniqueVFilePathBuilder;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.io.File;

/**
 * @author yole
 */
@ExtensionImpl
public class UniqueNameEditorTabTitleProvider implements EditorTabTitleProvider {
  private final Provider<UniqueVFilePathBuilder> myUniqueVFilePathBuilder;

  @Inject
  public UniqueNameEditorTabTitleProvider(Provider<UniqueVFilePathBuilder> uniqueVFilePathBuilder) {
    myUniqueVFilePathBuilder = uniqueVFilePathBuilder;
  }

  @Override
  public String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
    UISettings uiSettings = UISettings.getInstanceOrNull();
    if (uiSettings == null || !uiSettings.getShowDirectoryForNonUniqueFilenames() || DumbService.isDumb(project)) {
      return null;
    }

    UniqueVFilePathBuilder uniqueVFilePathBuilder = myUniqueVFilePathBuilder.get();
    // Even though this is a 'tab title provider' it is used also when tabs are not shown, namely for building IDE frame title.
    return ReadAction.compute(() -> {
      String uniqueName = uiSettings.getEditorTabPlacement() == UISettings.PLACEMENT_EDITOR_TAB_NONE
        ? uniqueVFilePathBuilder.getUniqueVirtualFilePath(project, file)
        : uniqueVFilePathBuilder.getUniqueVirtualFilePathWithinOpenedFileEditors(project, file);
      uniqueName = getEditorTabText(uniqueName, File.separator, uiSettings.getHideKnownExtensionInTabs());
      return uniqueName.equals(file.getName()) ? null : uniqueName;
    });
  }

  public static String getEditorTabText(String result, String separator, boolean hideKnownExtensionInTabs) {
    if (hideKnownExtensionInTabs) {
      String withoutExtension = FileUtil.getNameWithoutExtension(result);
      if (StringUtil.isNotEmpty(withoutExtension) && !withoutExtension.endsWith(separator)) {
        return withoutExtension;
      }
    }
    return result;
  }
}
