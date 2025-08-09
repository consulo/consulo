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
package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.daemon.impl.ShowIntentionsPass;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.internal.intention.ShowIntentionInternal;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.function.BiPredicate;

/**
 * @author VISTALL
 * @since 2025-08-09
 */
@Singleton
@ServiceImpl
public class ShowIntentionInternalImpl implements ShowIntentionInternal {
    @Override
    public boolean markActionInvoked(@Nonnull Project project, @Nonnull Editor editor, @Nonnull IntentionAction action) {
        return ShowIntentionsPass.markActionInvoked(project, editor, action);
    }

    @Nullable
    @Override
    public Pair<PsiFile, Editor> chooseBetweenHostAndInjected(@Nonnull PsiFile hostFile,
                                                              @Nonnull Editor hostEditor,
                                                              @Nullable PsiFile injectedFile,
                                                              @RequiredReadAction @Nonnull BiPredicate<? super PsiFile, ? super Editor> predicate) {
        return ShowIntentionActionsHandler.chooseBetweenHostAndInjected(hostFile, hostEditor, injectedFile, predicate);
    }

    @RequiredReadAction
    @Override
    public boolean availableFor(@Nonnull PsiFile psiFile, @Nonnull Editor editor, @Nonnull IntentionAction action) {
        return ShowIntentionActionsHandler.availableFor(psiFile, editor, action);
    }
}
