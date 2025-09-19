/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.generation.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.ide.impl.idea.codeInsight.generation.AutoIndentLinesHandler;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ActionImpl(id = "AutoIndentLines")
public class AutoIndentLinesAction extends BaseCodeInsightAction implements DumbAware {
    public AutoIndentLinesAction() {
        super(ActionLocalize.actionAutoindentlinesText(), ActionLocalize.actionAutoindentlinesDescription());
    }

    @Nullable
    @Override
    @RequiredUIAccess
    protected Editor getEditor(@Nonnull DataContext dataContext, @Nonnull Project project, boolean forUpdate) {
        Editor editor = getBaseEditor(dataContext, project);
        if (editor == null) {
            return null;
        }
        Document document = editor.getDocument();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        PsiFile psiFile = documentManager.getCachedPsiFile(document);
        if (psiFile == null) {
            return editor;
        }
        if (!forUpdate) {
            documentManager.commitDocument(document);
        }
        int startLineOffset = DocumentUtil.getLineStartOffset(editor.getSelectionModel().getSelectionStart(), document);
        return InjectedEditorManager.getInstance(project).getEditorForInjectedLanguageNoCommit(editor, psiFile, startLineOffset);
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new AutoIndentLinesHandler();
    }

    @Override
    @RequiredReadAction
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        FileType fileType = file.getFileType();
        return fileType instanceof LanguageFileType languageFileType
            && FormattingModelBuilder.forContext(languageFileType.getLanguage(), file) != null;
    }
}