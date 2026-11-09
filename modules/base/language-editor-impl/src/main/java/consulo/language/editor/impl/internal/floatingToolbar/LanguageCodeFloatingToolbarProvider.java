/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.floatingToolbar;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarConfiguration;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarProvider;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarSelection;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.floatingToolbar.CodeFloatingToolbarLanguageSupport;
import consulo.language.editor.template.TemplateManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-11-09
 */
@ExtensionImpl
public class LanguageCodeFloatingToolbarProvider implements CodeFloatingToolbarProvider {
    @Override
    @RequiredReadAction
    public @Nullable CodeFloatingToolbarConfiguration getConfiguration(Editor editor, CodeFloatingToolbarSelection selection) {
        Project project = editor.getProject();
        if (project == null || TemplateManager.getInstance(project).getActiveTemplate(editor) != null) {
            return null;
        }

        Document document = editor.getDocument();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        PsiFile file = documentManager.getPsiFile(document);
        if (file == null || !documentManager.isCommitted(document)) {
            return null;
        }

        PsiElement elementAtStart = PsiUtilCore.getElementAtOffset(file, selection.start());
        PsiElement elementAtEnd = PsiUtilCore.getElementAtOffset(file, selection.end());
        if (!elementAtStart.isWritable() || !elementAtEnd.isWritable()) {
            return null;
        }

        PsiElement elementAtCaret = file.findElementAt(selection.caretOffset());
        if (elementAtCaret == null) {
            return null;
        }

        Language language = PsiUtilCore.findLanguageFromElement(elementAtCaret);
        CodeFloatingToolbarLanguageSupport support = CodeFloatingToolbarLanguageSupport.forLanguage(language);
        if (support == null || !support.isToolbarAvailable()) {
            return null;
        }

        if (!selection.hasSelection() && support.isSelectionRequired()) {
            return null;
        }

        List<String> actionIds = new ArrayList<>(2);
        if (!support.isIntentionsGroupHidden()) {
            actionIds.add(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
        }
        actionIds.add(support.getActionGroupId());
        return new CodeFloatingToolbarConfiguration(actionIds, support.isConfigurationsGroupHidden());
    }
}
