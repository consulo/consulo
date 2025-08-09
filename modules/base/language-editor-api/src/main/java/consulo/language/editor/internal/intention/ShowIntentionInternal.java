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
package consulo.language.editor.internal.intention;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiPredicate;

/**
 * @author VISTALL
 * @since 2025-08-09
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ShowIntentionInternal {
    static ShowIntentionInternal getInstance() {
        return Application.get().getInstance(ShowIntentionInternal.class);
    }

    boolean markActionInvoked(@Nonnull Project project, @Nonnull Editor editor, @Nonnull IntentionAction action);

    @Nullable
    Pair<PsiFile, Editor> chooseBetweenHostAndInjected(
        @Nonnull PsiFile hostFile,
        @Nonnull Editor hostEditor,
        @Nullable PsiFile injectedFile,
        @RequiredReadAction @Nonnull BiPredicate<? super PsiFile, ? super Editor> predicate
    );

    @RequiredReadAction
    boolean availableFor(@Nonnull PsiFile psiFile, @Nonnull Editor editor, @Nonnull IntentionAction action);
}
