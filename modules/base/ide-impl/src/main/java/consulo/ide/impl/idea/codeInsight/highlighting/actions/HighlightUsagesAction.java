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
package consulo.ide.impl.idea.codeInsight.highlighting.actions;

import consulo.application.dumb.DumbAware;
import consulo.application.dumb.IndexNotReadyException;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInsight.highlighting.HighlightUsagesHandler;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;

public class HighlightUsagesAction extends AnAction implements DumbAware {
    public HighlightUsagesAction() {
        setInjectedContext(true);
    }

    @Override
    @RequiredUIAccess
    public void update(final AnActionEvent event) {
        final Presentation presentation = event.getPresentation();
        final DataContext dataContext = event.getDataContext();
        presentation.setEnabled(dataContext.getData(Project.KEY) != null && dataContext.getData(Editor.KEY) != null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        final Editor editor = e.getDataContext().getData(Editor.KEY);
        final Project project = e.getDataContext().getData(Project.KEY);
        if (editor == null || project == null) {
            return;
        }

        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(getTemplatePresentation().getTextValue())
            .run(() -> {
                PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
                try {
                    HighlightUsagesHandler.invoke(project, editor, psiFile);
                }
                catch (IndexNotReadyException ex) {
                    DumbService.getInstance(project)
                        .showDumbModeNotification(ActionLocalize.actionHighlightusagesinfileNotReady());
                }
            });
    }
}
