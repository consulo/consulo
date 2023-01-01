// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.diff.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.EditorTabColorProvider;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.editor.FileColorManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

@ExtensionImpl
public class VcsEditorTabColorProvider implements EditorTabColorProvider, DumbAware {

  @Nullable
  @Override
  public Color getEditorTabColor(@Nonnull Project project, @Nonnull VirtualFile file) {
    if (file instanceof DiffVirtualFile) {
      FileColorManager fileColorManager = FileColorManager.getInstance(project);
      if (file.getName().equals("Shelf")) {
        return fileColorManager.getColor("Violet");
      }

      return fileColorManager.getColor("Green");
    }

    return null;
  }
}
