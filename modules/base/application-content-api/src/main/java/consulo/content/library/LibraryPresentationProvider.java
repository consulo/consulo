/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.content.library;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author nik
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class LibraryPresentationProvider<P extends LibraryProperties> implements LibraryPresentation<P> {
    public static final ExtensionPointName<LibraryPresentationProvider> EP_NAME =
        ExtensionPointName.create(LibraryPresentationProvider.class);
    private final LibraryKind myKind;

    protected LibraryPresentationProvider(@Nonnull LibraryKind kind) {
        myKind = kind;
    }

    @Nonnull
    @Override
    public LibraryKind getKind() {
        return myKind;
    }

    @Nullable
    @Override
    public abstract Image getIcon();

    @Nullable
    @Override
    public String getDescription(@Nonnull P properties) {
        return null;
    }

    @Nullable
    @Override
    public abstract P detect(@Nonnull List<VirtualFile> classesRoots);
}
