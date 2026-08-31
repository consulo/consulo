// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.language.index.impl.internal.roots.kind.ProjectFileOrDirOrigin;
import consulo.virtualFileSystem.VirtualFile;

class ProjectFileOrDirOriginImpl implements ProjectFileOrDirOrigin {
    private final VirtualFile myFileOrDir;

    ProjectFileOrDirOriginImpl(VirtualFile fileOrDir) {
        myFileOrDir = fileOrDir;
    }

    @Override
    public VirtualFile getFileOrDir() {
        return myFileOrDir;
    }
}
