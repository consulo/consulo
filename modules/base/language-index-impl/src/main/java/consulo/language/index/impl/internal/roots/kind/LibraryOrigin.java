// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots.kind;

import consulo.content.library.Library;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;

public interface LibraryOrigin extends IndexableSetOrigin {
    Library getLibrary();

    List<VirtualFile> getClassRoots();

    List<VirtualFile> getSourceRoots();
}
