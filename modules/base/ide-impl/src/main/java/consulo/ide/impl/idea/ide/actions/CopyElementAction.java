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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.refactoring.copy.CopyHandler;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

@ActionImpl(id = "CopyElement")
public class CopyElementAction extends AnAction {
    @Inject
    public CopyElementAction() {
        super(ActionLocalize.actionCopyelementText(), ActionLocalize.actionCopyelementDescription());
    }

    public CopyElementAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = dataContext.getRequiredData(Project.KEY);

        CommandProcessor.getInstance().newCommand()
            .project(project)
            .run(() -> PsiDocumentManager.getInstance(project).commitAllDocuments());
        Editor editor = dataContext.getData(Editor.KEY);
        PsiElement[] elements;

        PsiDirectory defaultTargetDirectory;
        if (editor != null) {
            PsiElement aElement = getTargetElement(editor, project);
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null) {
                return;
            }
            elements = new PsiElement[]{aElement};
            if (aElement == null || !CopyHandler.canCopy(elements)) {
                elements = new PsiElement[]{file};
            }
            defaultTargetDirectory = file.getContainingDirectory();
        }
        else {
            PsiElement element = dataContext.getData(LangDataKeys.TARGET_PSI_ELEMENT);
            defaultTargetDirectory = element instanceof PsiDirectory directory ? directory : null;
            elements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
        }
        doCopy(elements, defaultTargetDirectory);
    }

    protected void doCopy(PsiElement[] elements, PsiDirectory defaultTargetDirectory) {
        CopyHandler.doCopy(elements, defaultTargetDirectory);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Project project = e.getData(Project.KEY);
        presentation.setEnabled(false);
        if (project == null) {
            return;
        }

        Editor editor = e.getData(Editor.KEY);
        if (editor != null) {
            updateForEditor(e.getDataContext(), presentation);
        }
        else {
            String id = ToolWindowManager.getInstance(project).getActiveToolWindowId();
            updateForToolWindow(id, e.getDataContext(), presentation);
        }
    }

    @RequiredUIAccess
    protected void updateForEditor(DataContext dataContext, Presentation presentation) {
        Editor editor = dataContext.getData(Editor.KEY);
        if (editor == null) {
            presentation.setVisible(false);
            return;
        }

        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return;
        }
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

        PsiElement element = getTargetElement(editor, project);
        boolean result = element != null && CopyHandler.canCopy(new PsiElement[]{element});

        if (!result && file != null) {
            result = CopyHandler.canCopy(new PsiElement[]{file});
        }

        presentation.setEnabled(result);
        presentation.setVisible(true);
    }

    protected void updateForToolWindow(String toolWindowId, DataContext dataContext, Presentation presentation) {
        PsiElement[] elements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
        presentation.setEnabled(elements != null && CopyHandler.canCopy(elements));
    }

    @RequiredUIAccess
    private static PsiElement getTargetElement(Editor editor, Project project) {
        int offset = editor.getCaretModel().getOffset();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) {
            return null;
        }
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            element = file;
        }
        return element;
    }
}
