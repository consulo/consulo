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

import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.localize.LocalizeValue;
import consulo.ui.InputBoxBuilder;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author nik
 */
public abstract class CompositePackagingElementType<E extends CompositePackagingElement<?>> extends PackagingElementType<E> {
    protected CompositePackagingElementType(String id, LocalizeValue presentableName) {
        super(id, presentableName);
    }

    @Override
    public boolean isAvailableForAdd(ArtifactEditorContext context, Artifact artifact) {
        return true;
    }

    @RequiredUIAccess
    public abstract CompletableFuture<CompositePackagingElement<?>> createComposite(
        CompositePackagingElement<?> parent,
        @Nullable String baseName,
        ArtifactEditorContext context
    );

    @Override
    @RequiredUIAccess
    public CompletableFuture<List<? extends PackagingElement<?>>> chooseAndCreate(
        ArtifactEditorContext context,
        Artifact artifact,
        CompositePackagingElement<?> parent
    ) {
        return createComposite(parent, null, context).<List<? extends PackagingElement<?>>>thenApply(composite -> List.of(composite));
    }

    @RequiredUIAccess
    protected CompletableFuture<CompositePackagingElement<?>> createCompositeByPath(
        CompositePackagingElement<?> parent,
        ArtifactEditorContext context,
        LocalizeValue title,
        LocalizeValue prompt,
        String baseName,
        String extension,
        Function<String, PackagingElement<?>> elementFactory
    ) {
        return InputBoxBuilder.text()
            .title(title)
            .text(prompt)
            .value(ArtifactUtil.suggestFileName(parent, baseName, extension))
            .validator(new FilePathValidator())
            .showAsync()
            .thenApply(path -> {
                String independentPath = FileUtil.toSystemIndependentName(path);
                PackagingElement<?> element = elementFactory.apply(PathUtil.getFileName(independentPath));
                PackagingElementFactory factory = PackagingElementFactory.getInstance(context.getProject());
                return (CompositePackagingElement<?>) factory.createParentDirectories(PathUtil.getParentPath(independentPath), element);
            });
    }
}
