// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.diff.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.diff.impl.internal.editor.DiffVirtualFile;
import consulo.fileEditor.EditorTabColorProvider;
import consulo.language.editor.FileColorManager;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public class DiffEditorTabColorProvider implements EditorTabColorProvider, DumbAware {

  @Nullable
  @Override
  public ColorValue getEditorTabColor(@Nonnull Project project, @Nonnull VirtualFile file) {
    if (file instanceof DiffVirtualFile) {
      FileColorManager fileColorManager = FileColorManager.getInstance(project);
      if (file.getName().equals("Shelf")) {
        return TargetAWT.from(fileColorManager.getColor("Violet"));
      }

      return TargetAWT.from(fileColorManager.getColor("Green"));
    }

    return null;
  }
}
