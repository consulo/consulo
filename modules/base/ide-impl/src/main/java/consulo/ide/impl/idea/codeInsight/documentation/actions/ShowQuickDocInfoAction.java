/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.documentation.actions;

import consulo.application.dumb.DumbAware;
import consulo.application.dumb.IndexNotReadyException;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutter;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

public class ShowQuickDocInfoAction extends BaseCodeInsightAction implements HintManagerImpl.ActionToIgnore, DumbAware, PopupAction {
    @SuppressWarnings("SpellCheckingInspection")
    public static final String CODEASSISTS_QUICKJAVADOC_FEATURE = "codeassists.quickjavadoc";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String CODEASSISTS_QUICKJAVADOC_LOOKUP_FEATURE = "codeassists.quickjavadoc.lookup";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String CODEASSISTS_QUICKJAVADOC_CTRLN_FEATURE = "codeassists.quickjavadoc.ctrln";

    public ShowQuickDocInfoAction() {
        setEnabledInModalContext(true);
        setInjectedContext(true);
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new CodeInsightActionHandler() {
            @RequiredUIAccess
            @Override
            public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
                DocumentationManager.getInstance(project).showJavaDocInfo(editor, file, LookupManager.getActiveLookup(editor) == null);
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        };
    }

    @Override
    protected boolean isValidForLookup() {
        return true;
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();

        Project project = event.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }

        Editor editor = event.getData(Editor.KEY);
        PsiElement element = event.getData(PsiElement.KEY);
        if (editor == null && element == null) {
            presentation.setEnabled(false);
            return;
        }

        if (LookupManager.getInstance(project).getActiveLookup() != null) {
            if (!isValidForLookup()) {
                presentation.setEnabled(false);
            }
            else {
                presentation.setEnabled(true);
            }
        }
        else {
            if (editor != null) {
                if (event.getData(EditorGutter.KEY) != null) {
                    presentation.setEnabled(false);
                    return;
                }
                PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
                if (file == null) {
                    presentation.setEnabled(false);
                }

                if (element == null && file != null) {
                    try {
                        final PsiReference ref = file.findReferenceAt(editor.getCaretModel().getOffset());
                        if (ref instanceof PsiPolyVariantReference) {
                            element = ref.getElement();
                        }
                    }
                    catch (IndexNotReadyException e) {
                        element = null;
                    }
                }
            }

            if (element != null) {
                presentation.setEnabled(true);
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        final Editor editor = e.getData(Editor.KEY);
        final PsiElement element = e.getData(PsiElement.KEY);

        if (project != null && editor != null) {
            FeatureUsageTracker.getInstance().triggerFeatureUsed(CODEASSISTS_QUICKJAVADOC_FEATURE);
            final LookupEx lookup = LookupManager.getInstance(project).getActiveLookup();
            if (lookup != null) {
                //dumpLookupElementWeights(lookup);
                FeatureUsageTracker.getInstance().triggerFeatureUsed(CODEASSISTS_QUICKJAVADOC_LOOKUP_FEATURE);
            }
            actionPerformedImpl(project, editor);
        }
        else if (project != null && element != null) {
            FeatureUsageTracker.getInstance().triggerFeatureUsed(CODEASSISTS_QUICKJAVADOC_CTRLN_FEATURE);
            CommandProcessor.getInstance().newCommand(() -> DocumentationManager.getInstance(project).showJavaDocInfo(element, null))
                .withProject(project)
                .withName(getTemplatePresentation().getTextValue())
                .execute();
        }
    }
}
