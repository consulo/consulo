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
package consulo.compiler.artifact.element;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.localize.CompilerLocalize;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author nik
 */
@ExtensionImpl
public class DirectoryCopyElementType extends PackagingElementType<DirectoryCopyPackagingElement> {
    public static DirectoryCopyElementType getInstance() {
        return getInstance(DirectoryCopyElementType.class);
    }

    public DirectoryCopyElementType() {
        super("dir-copy", CompilerLocalize.elementTypeNameDirectoryContent());
    }

    @Override
    public Image getIcon() {
        return AllIcons.Nodes.CopyOfFolder;
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<List<? extends PackagingElement<?>>> chooseAndCreate(
        ArtifactEditorContext context,
        Artifact artifact,
        CompositePackagingElement<?> parent
    ) {
        return FileChooser.chooseFiles(FileChooserDescriptorFactory.createMultipleFoldersDescriptor(), context.getProject(), null)
            .thenApply(files -> {
                List<DirectoryCopyPackagingElement> list = new ArrayList<>();
                for (VirtualFile file : files) {
                    list.add(new DirectoryCopyPackagingElement(file.getPath()));
                }
                return list;
            });
    }

    @Override
    public DirectoryCopyPackagingElement createEmpty(Project project) {
        return new DirectoryCopyPackagingElement();
    }
}
