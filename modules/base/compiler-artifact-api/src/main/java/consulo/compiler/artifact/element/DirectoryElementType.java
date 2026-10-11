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
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.localize.CompilerLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author nik
 */
@ExtensionImpl
public class DirectoryElementType extends CompositePackagingElementType<DirectoryPackagingElement> {
    public static DirectoryElementType getInstance() {
        return getInstance(DirectoryElementType.class);
    }

    public DirectoryElementType() {
        super("directory", CompilerLocalize.elementTypeNameDirectory());
    }

    @Override
    public Image getIcon() {
        return AllIcons.Actions.NewFolder;
    }

    @Override
    public DirectoryPackagingElement createEmpty(Project project) {
        return new DirectoryPackagingElement();
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<CompositePackagingElement<?>> createComposite(
        CompositePackagingElement<?> parent,
        @Nullable String baseName,
        ArtifactEditorContext context
    ) {
        return createCompositeByPath(
            parent,
            context,
            CompilerLocalize.dialogTitleNewDirectory(),
            CompilerLocalize.dialogMessageEnterDirectoryName(),
            baseName != null ? baseName : "folder",
            "",
            DirectoryPackagingElement::new
        );
    }
}
