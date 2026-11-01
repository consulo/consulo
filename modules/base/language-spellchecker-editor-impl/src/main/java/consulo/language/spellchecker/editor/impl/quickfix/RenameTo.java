/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.spellchecker.editor.impl.quickfix;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.refactoring.rename.NameSuggestionProvider;
import consulo.language.editor.refactoring.rename.RenameElementAction;
import consulo.language.editor.refactoring.rename.RenameHandlerRegistry;
import consulo.language.inject.InjectedLanguageManagerUtil;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import org.jspecify.annotations.Nullable;

public class RenameTo extends ShowSuggestions implements SpellCheckerQuickFix {
    public RenameTo(String wordWithTypo) {
        super(wordWithTypo);
    }

    @Override
    public LocalizeValue getName() {
        return SpellCheckerLocalize.renameTo();
    }

    @Override
    @RequiredUIAccess
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        DictionarySuggestionProvider provider = project.getApplication().getExtensionPoint(NameSuggestionProvider.class)
            .findExtension(DictionarySuggestionProvider.class);
        if (provider != null) {
            provider.setActive(true);
        }

        PsiElement psiElement = descriptor.getPsiElement();
        if (psiElement == null) {
            return;
        }

        Editor editor = getEditor(psiElement, project);

        if (editor == null) {
            return;
        }

        DataContext.Builder builder = DataContext.builder();
        if (editor instanceof EditorWindow) {
            builder.add(Editor.KEY, editor);
            builder.add(PsiElement.KEY, psiElement);
        }

        Boolean selectAll = editor.getUserData(RenameHandlerRegistry.SELECT_ALL);
        try {
            editor.putUserData(RenameHandlerRegistry.SELECT_ALL, true);

            builder.parent(DataManager.getInstance().getDataContext(editor.getComponent()));

            AnAction action = new RenameElementAction();
            AnActionEvent event =
                new AnActionEvent(null, builder.build(), "", action.getTemplatePresentation(), ActionManager.getInstance(), 0);
            action.actionPerformed(event);
            if (provider != null) {
                provider.setActive(false);
            }
        }
        finally {
            editor.putUserData(RenameHandlerRegistry.SELECT_ALL, selectAll);
        }
    }

    @Nullable
    @RequiredReadAction
    protected Editor getEditor(PsiElement element, Project project) {
        return InjectedLanguageManagerUtil.findInjectionHost(element) != null
            ? InjectedEditorManager.getInstance(project).openEditorFor(element.getContainingFile())
            : FileEditorManager.getInstance(project).getSelectedTextEditor();
    }
}