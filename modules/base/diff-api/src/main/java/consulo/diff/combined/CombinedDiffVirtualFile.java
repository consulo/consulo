// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.diff.combined;

import consulo.diff.DiffViewerVirtualFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

public abstract class CombinedDiffVirtualFile extends DiffViewerVirtualFile {
    private final String myPath;

    public CombinedDiffVirtualFile(String name) {
        this(name, name);
    }

    public CombinedDiffVirtualFile(String name, String path) {
        super(name);
        myPath = path;
    }

    @Override
    public String getPath() {
        return myPath;
    }

    @RequiredUIAccess
    @Override
    public abstract CombinedDiffComponentProcessor createViewer(Project project);
}
