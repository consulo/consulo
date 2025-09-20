// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

public interface RangeSearchCreator {

    @Nonnull
    RangeSearch createContent(Project project,
                              VirtualFile virtualFile,
                              String titleName);
}
