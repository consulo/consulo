/*
 * Copyright 2013-2016 consulo.io
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
 * @author VISTALL
 * @since 16:04/18.06.13
 */
@ExtensionImpl(id = "zip-archive-element")
public class ZipArchiveElementType extends CompositePackagingElementType<ZipArchivePackagingElement> {
    public static ZipArchiveElementType getInstance() {
        return getInstance(ZipArchiveElementType.class);
    }

    public ZipArchiveElementType() {
        super("zip-archive", CompilerLocalize.elementTypeNameZipArchive());
    }

    @Override
    public Image getIcon() {
        return AllIcons.Nodes.PpJar;
    }

    @Override
    public ZipArchivePackagingElement createEmpty(Project project) {
        return new ZipArchivePackagingElement();
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
            CompilerLocalize.dialogTitleNewArchive(),
            CompilerLocalize.dialogMessageEnterArchiveName(),
            baseName != null ? baseName : "archive",
            ".zip",
            ZipArchivePackagingElement::new
        );
    }
}
