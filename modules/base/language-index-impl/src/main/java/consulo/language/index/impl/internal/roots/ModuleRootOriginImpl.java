// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.language.index.impl.internal.roots.kind.ModuleRootOrigin;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;
import java.util.Objects;

class ModuleRootOriginImpl implements ModuleRootOrigin {
    private final Module myModule;
    private final List<VirtualFile> myRoots;

    ModuleRootOriginImpl(Module module, List<VirtualFile> roots) {
        myModule = module;
        myRoots = roots;
    }

    @Override
    public Module getModule() {
        return myModule;
    }

    @Override
    public List<VirtualFile> getRoots() {
        return myRoots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModuleRootOriginImpl other)) {
            return false;
        }
        return myModule.equals(other.myModule) && myRoots.equals(other.myRoots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myModule, myRoots);
    }
}
