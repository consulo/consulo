// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.core.local.CoreLocalFileSystem;
import consulo.virtualFileSystem.internal.core.local.CoreLocalVirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class PreloadedDirectory extends CoreLocalVirtualFile {
    private final List<LazyDirectoryOrFile> myChildren = new ArrayList<>();

    PreloadedDirectory(CoreLocalFileSystem fileSystem, Path file) {
        super(fileSystem, file, true);
    }

    void addChild(LazyDirectoryOrFile child) {
        myChildren.add(child);
    }

    @Override
    public @Nullable VirtualFile getParent() {
        return null;
    }

    @Override
    public @Nullable VirtualFile findChild(String name) {
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(name);
        }
        for (LazyDirectoryOrFile child : myChildren) {
            if (name.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    @Override
    public VirtualFile[] getChildren() {
        return myChildren.toArray(VirtualFile.EMPTY_ARRAY);
    }
}
