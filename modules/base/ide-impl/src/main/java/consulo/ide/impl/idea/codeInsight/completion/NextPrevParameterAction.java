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

import consulo.application.Application;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.action.CodeInsightAction;
import consulo.codeEditor.Editor;
import consulo.language.editor.template.TemplateParameterTraversalPolicy;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;

import org.jspecify.annotations.Nullable;

/**
 * @author max
 * @author Evgeny Gerashchenko
 */
public abstract class NextPrevParameterAction extends CodeInsightAction {
    private final boolean myNext;

    protected NextPrevParameterAction(LocalizeValue text, boolean next) {
        super(text, text);
        myNext = next;
    }

    @Override
    public CodeInsightActionHandler getHandler() {
        return new Handler();
    }

    @Override
    protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        return hasSuitablePolicy(editor, file);
    }

    public static boolean hasSuitablePolicy(Editor editor, PsiFile file) {
        return findSuitableTraversalPolicy(editor, file) != null;
    }

    @Deprecated(forRemoval = true)
    public static boolean hasSutablePolicy(Editor editor, PsiFile file) {
        return hasSuitablePolicy(editor, file);
    }

    private static @Nullable TemplateParameterTraversalPolicy findSuitableTraversalPolicy(Editor editor, PsiFile file) {
        return Application.get().getExtensionPoint(TemplateParameterTraversalPolicy.class)
            .findFirstSafe(policy -> policy.isValidForFile(editor, file));
    }

    private class Handler implements CodeInsightActionHandler {
        @Override
        @RequiredUIAccess
        public void invoke(Project project, Editor editor, PsiFile file) {
            TemplateParameterTraversalPolicy policy = findSuitableTraversalPolicy(editor, file);
            if (policy != null) {
                policy.invoke(editor, file, myNext);
            }
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }
}
