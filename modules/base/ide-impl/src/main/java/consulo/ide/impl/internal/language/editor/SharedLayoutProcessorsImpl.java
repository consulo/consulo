/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.internal.language.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.codeInsight.actions.OptimizeImportsProcessor;
import consulo.ide.impl.idea.codeInsight.actions.RearrangeCodeProcessor;
import consulo.ide.impl.idea.codeInsight.actions.ReformatCodeProcessor;
import consulo.language.editor.impl.inspection.GlobalInspectionContextBase;
import consulo.language.editor.internal.LayoutCodeProcessor;
import consulo.language.editor.internal.SharedLayoutProcessors;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-09-13
 */
@ServiceImpl
@Singleton
public class SharedLayoutProcessorsImpl implements SharedLayoutProcessors {
    private final Project myProject;

    @Inject
    public SharedLayoutProcessorsImpl(Project project) {
        myProject = project;
    }

    @Override
    @RequiredReadAction
    public LayoutCodeProcessor createOptimizeImportsProcessor(PsiFile[] files, LocalizeValue commandName, Runnable postRunnable) {
        return new OptimizeImportsProcessor(myProject, files, commandName, postRunnable);
    }

    @Override
    @RequiredReadAction
    public LayoutCodeProcessor createRearrangeCodeProcessor(PsiFile[] files, LocalizeValue commandName, Runnable postRunnable) {
        return new RearrangeCodeProcessor(myProject, files, commandName, postRunnable);
    }

    @Override
    @RequiredReadAction
    public LayoutCodeProcessor createReformatCodeProcessor(PsiFile[] files, LocalizeValue commandName, Runnable postRunnable) {
        return new ReformatCodeProcessor(myProject, files, commandName, postRunnable, true);
    }

    @Override
    public LayoutCodeProcessor createCodeCleanupProcessor(AnalysisScope scope, @Nullable Runnable postRunnable) {
        return () -> GlobalInspectionContextBase.codeCleanup(myProject, scope, postRunnable);
    }
}
