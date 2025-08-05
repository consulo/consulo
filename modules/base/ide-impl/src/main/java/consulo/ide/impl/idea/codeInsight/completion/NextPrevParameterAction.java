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
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.action.CodeInsightAction;
import consulo.codeEditor.Editor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 * @author Evgeny Gerashchenko
 */
public abstract class NextPrevParameterAction extends CodeInsightAction {
    private final boolean myNext;

    protected NextPrevParameterAction(@Nonnull LocalizeValue text, boolean next) {
        super(text, LocalizeValue.empty());
        myNext = next;
    }

    @Nonnull
    @Override
    public CodeInsightActionHandler getHandler() {
        return new Handler();
    }

    @Override
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return hasSutablePolicy(editor, file);
    }

    public static boolean hasSutablePolicy(Editor editor, PsiFile file) {
        return findSuitableTraversalPolicy(editor, file) != null;
    }

    @Nullable
    private static TemplateParameterTraversalPolicy findSuitableTraversalPolicy(Editor editor, PsiFile file) {
        for (TemplateParameterTraversalPolicy policy : TemplateParameterTraversalPolicy.EP_NAME.getExtensionList()) {
            if (policy.isValidForFile(editor, file)) {
                return policy;
            }
        }
        return null;
    }

    private class Handler implements CodeInsightActionHandler {
        @Override
        @RequiredUIAccess
        public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
            TemplateParameterTraversalPolicy policy = findSuitableTraversalPolicy(editor, file);
            if (policy != null) {
                PsiDocumentManager.getInstance(project).commitAllDocuments();

                policy.invoke(editor, file, myNext);
            }
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }
}
