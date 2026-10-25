// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.diff;

import consulo.fileEditor.history.SkipFromDocumentHistory;
import consulo.language.file.light.LightVirtualFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFileWithoutContent;

public abstract class DiffViewerVirtualFile extends LightVirtualFile implements VirtualFileWithoutContent, DiffContentVirtualFile, SkipFromDocumentHistory {
    public DiffViewerVirtualFile(String name) {
        super(name, DiffFileType.INSTANCE, "");
    }

    @RequiredUIAccess
    public abstract DiffEditorViewer createViewer(Project project);

    @Override
    public boolean isWritable() {
        return false;
    }
}
