/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.awt.internal.diff.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.diff.request.DiffRequest;
import consulo.fileEditor.EditorTabTitleProvider;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.diff.impl.internal.DiffSettingsHolder;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

// from kotlin
@ExtensionImpl
public class DiffEditorTabTitleProvider implements EditorTabTitleProvider, DumbAware {
  @Override
  public String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
    String title = getTitle(project, file);

    if (title != null) {
      DiffSettingsHolder.DiffSettings settings = DiffSettingsHolder.DiffSettings.getSettings();

      if (settings.isShowDiffInEditor()) {
        return shorten(title, 30);
      }
    }
    return title;
  }

  private static String shorten(String title, int maxLength) {
    if (title.length() < maxLength) {
      return title;
    }

    int index = title.indexOf("(");

    if (index >= 1 && index <= maxLength) {
      return title.substring(0, index);
    }

    return StringUtil.shortenTextWithEllipsis(title, maxLength, 0);
  }

  public String getTitle(Project project, VirtualFile file) {
    if (!(file instanceof ChainDiffVirtualFile)) {
      return null;
    }

    FileEditor editor = FileEditorManager.getInstance(project).getSelectedEditor(file);
    if (editor instanceof DiffRequestProcessorEditor diffRequestProcessorEditor) {
      DiffRequest activeRequest = diffRequestProcessorEditor.getProcessor().getActiveRequest();
      if (activeRequest != null) {
        return activeRequest.getTitle();
      }
    }

    return null;
  }
}
