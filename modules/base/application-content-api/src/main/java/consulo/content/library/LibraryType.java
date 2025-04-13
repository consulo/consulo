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
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.ui.LibraryEditorComponent;
import consulo.content.library.ui.LibraryPropertiesEditor;
import consulo.content.library.ui.LibraryRootsComponentDescriptor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author nik
 * @see ModuleAwareLibraryType
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class LibraryType<P extends LibraryProperties> implements LibraryPresentation<P> {
    public static final ExtensionPointName<LibraryType> EP_NAME = ExtensionPointName.create(LibraryType.class);

    @Nonnull
    public static OrderRootType[] getDefaultExternalRootTypes() {
        return new OrderRootType[]{BinariesOrderRootType.getInstance()};
    }

    private final PersistentLibraryKind<P> myKind;

    protected LibraryType(@Nonnull PersistentLibraryKind<P> libraryKind) {
        myKind = libraryKind;
    }

    @Nonnull
    @Override
    public final PersistentLibraryKind<P> getKind() {
        return myKind;
    }

    /**
     * @return text to show in 'New Library' popup. Return {@code null} if the type should not be shown in the 'New Library' popup
     */
    @Nullable
    public abstract String getCreateActionName();

    /**
     * Called when a new library of this type is created in Project Structure dialog
     */
    @Nullable
    public NewLibraryConfiguration createNewLibrary(
        @Nonnull JComponent parentComponent,
        @Nullable VirtualFile contextDirectory,
        @Nonnull Project project
    ) {
        LibraryRootsComponentDescriptor descriptor = createLibraryRootsComponentDescriptor();
        if (descriptor == null) {
            return null;
        }
        return LibraryTypeService.getInstance().createLibraryFromFiles(descriptor, parentComponent, contextDirectory, this, project);
    }

    public boolean isAvailable() {
        return true;
    }

    /**
     * Override this method to customize the library roots editor
     *
     * @return {@link LibraryRootsComponentDescriptor} instance
     */
    @Nullable
    public LibraryRootsComponentDescriptor createLibraryRootsComponentDescriptor() {
        return null;
    }

    @Nullable
    public abstract LibraryPropertiesEditor createPropertiesEditor(@Nonnull LibraryEditorComponent<P> editorComponent);

    @Override
    public P detect(@Nonnull List<VirtualFile> classesRoots) {
        return null;
    }

    /**
     * @return Root types to collect library files which do not belong to the project and therefore
     * indicate that the library is external.
     */
    public OrderRootType[] getExternalRootTypes() {
        return getDefaultExternalRootTypes();
    }
}
