// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.language.index.impl.internal.roots.kind.ProjectFileOrDirOrigin;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

class ProjectFileOrDirOriginImpl implements ProjectFileOrDirOrigin {
    private final VirtualFile myFileOrDir;

    ProjectFileOrDirOriginImpl(VirtualFile fileOrDir) {
        myFileOrDir = fileOrDir;
    }

    @Override
    public VirtualFile getFileOrDir() {
        return myFileOrDir;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o
            || o instanceof ProjectFileOrDirOriginImpl that && myFileOrDir.equals(that.myFileOrDir);
    }

    @Override
    public int hashCode() {
        return myFileOrDir.hashCode();
    }
}
