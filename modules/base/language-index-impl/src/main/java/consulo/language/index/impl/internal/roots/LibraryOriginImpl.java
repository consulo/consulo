// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.roots;

import consulo.content.library.Library;
import consulo.language.index.impl.internal.roots.kind.LibraryOrigin;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;
import java.util.Objects;

class LibraryOriginImpl implements LibraryOrigin {
    private final Library myLibrary;
    private final List<VirtualFile> myClassRoots;
    private final List<VirtualFile> mySourceRoots;

    LibraryOriginImpl(Library library, List<VirtualFile> classRoots, List<VirtualFile> sourceRoots) {
        myLibrary = library;
        myClassRoots = classRoots;
        mySourceRoots = sourceRoots;
    }

    @Override
    public Library getLibrary() {
        return myLibrary;
    }

    @Override
    public List<VirtualFile> getClassRoots() {
        return myClassRoots;
    }

    @Override
    public List<VirtualFile> getSourceRoots() {
        return mySourceRoots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LibraryOriginImpl other)) {
            return false;
        }
        return myLibrary.equals(other.myLibrary)
            && myClassRoots.equals(other.myClassRoots)
            && mySourceRoots.equals(other.mySourceRoots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myLibrary, myClassRoots, mySourceRoots);
    }

    @Override
    public String toString() {
        return "LibraryOriginImpl(library=" + myLibrary.getName() + ", classRoots=" + myClassRoots + ", sourceRoots=" + mySourceRoots + ")";
    }
}
