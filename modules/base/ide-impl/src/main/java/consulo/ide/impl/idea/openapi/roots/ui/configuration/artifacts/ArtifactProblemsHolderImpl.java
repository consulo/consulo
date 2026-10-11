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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import consulo.compiler.artifact.ui.ArtifactProblemQuickFix;
import consulo.compiler.artifact.ui.ArtifactProblemsHolderBase;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactProblemsHolderImpl extends ArtifactProblemsHolderBase {
    private final List<ArtifactProblem> myProblems = new ArrayList<>();

    public ArtifactProblemsHolderImpl(PackagingElementResolvingContext context) {
        super(context);
    }

    @Override
    public void registerError(
        String message,
        String problemTypeId,
        @Nullable List<PackagingElement<?>> pathToPlace,
        ArtifactProblemQuickFix... quickFixes
    ) {
        myProblems.add(new ArtifactProblem(message, true, copyPath(pathToPlace), List.of(quickFixes)));
    }

    @Override
    public void registerWarning(
        String message,
        String problemTypeId,
        @Nullable List<PackagingElement<?>> pathToPlace,
        ArtifactProblemQuickFix... quickFixes
    ) {
        myProblems.add(new ArtifactProblem(message, false, copyPath(pathToPlace), List.of(quickFixes)));
    }

    public List<ArtifactProblem> getProblems() {
        return myProblems;
    }

    private static @Nullable List<PackagingElement<?>> copyPath(@Nullable List<PackagingElement<?>> pathToPlace) {
        return pathToPlace == null ? null : List.copyOf(pathToPlace);
    }
}
