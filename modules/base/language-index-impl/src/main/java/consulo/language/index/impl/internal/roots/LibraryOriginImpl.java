// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.content.library.Library;
import consulo.language.index.impl.internal.roots.kind.LibraryOrigin;

class LibraryOriginImpl implements LibraryOrigin {
    private final Library myLibrary;

    LibraryOriginImpl(Library library) {
        myLibrary = library;
    }

    @Override
    public Library getLibrary() {
        return myLibrary;
    }
}
