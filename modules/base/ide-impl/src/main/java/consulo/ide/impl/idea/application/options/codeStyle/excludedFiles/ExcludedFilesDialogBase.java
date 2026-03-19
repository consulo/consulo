// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.application.options.codeStyle.excludedFiles;

import consulo.language.codeStyle.fileSet.FileSetDescriptor;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import org.jspecify.annotations.Nullable;

public abstract class ExcludedFilesDialogBase extends DialogWrapper {

  protected ExcludedFilesDialogBase(@Nullable Project project) {
    super(project);
  }

  protected ExcludedFilesDialogBase(boolean canBeParent) {
    super(canBeParent);
  }

  public abstract @Nullable FileSetDescriptor getDescriptor();
}
