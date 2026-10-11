/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.artifact.element;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.localize.CompilerLocalize;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author nik
 */
@ExtensionImpl
public class LibraryElementType extends ComplexPackagingElementType<LibraryPackagingElement> {
    public static LibraryElementType getInstance() {
        return getInstance(LibraryElementType.class);
    }

    public LibraryElementType() {
        super("library", CompilerLocalize.elementTypeNameLibraryFiles());
    }

    @Override
    public Image getIcon() {
        return AllIcons.Nodes.PpLib;
    }

    @Override
    public boolean isAvailableForAdd(ArtifactEditorContext context, Artifact artifact) {
        return ProjectLibraryTable.getInstance(context.getProject()).getLibraries().length > 0;
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<List<? extends PackagingElement<?>>> chooseAndCreate(
        ArtifactEditorContext context,
        Artifact artifact,
        CompositePackagingElement<?> parent
    ) {
        return context.chooseLibraries(ProjectLocalize.dialogTitlePackagingChooseLibrary()).thenApply(selected -> {
            List<LibraryPackagingElement> elements = new ArrayList<>();
            for (Library library : selected) {
                LibraryTable table = library.getTable();
                if (table != null) {
                    elements.add(new LibraryPackagingElement(table.getTableLevel(), library.getName(), null));
                }
            }
            return elements;
        });
    }

    @Override
    public LibraryPackagingElement createEmpty(Project project) {
        return new LibraryPackagingElement();
    }

    @Override
    public LocalizeValue getShowContentActionText() {
        return CompilerLocalize.elementTypeLibraryShowContentActionText();
    }
}
