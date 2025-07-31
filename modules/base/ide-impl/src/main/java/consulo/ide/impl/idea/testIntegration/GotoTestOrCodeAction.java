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
package consulo.ide.impl.idea.testIntegration;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.testIntegration.TestFinderHelper;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "GotoTest")
public class GotoTestOrCodeAction extends BaseCodeInsightAction {
    public GotoTestOrCodeAction() {
        super(ActionLocalize.actionGototestText(), ActionLocalize.actionGototestDescription());
    }

    @Override
    @Nonnull
    protected CodeInsightActionHandler getHandler() {
        return new GotoTestOrCodeHandler();
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Presentation p = event.getPresentation();
        if (TestFinderHelper.getFinders().isEmpty()) {
            p.setVisible(false);
            return;
        }
        p.setEnabled(false);
        Project project = event.getData(Project.KEY);
        Editor editor = event.getData(Editor.KEY);
        if (editor == null || project == null) {
            return;
        }

        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (psiFile == null) {
            return;
        }

        PsiElement element = GotoTestOrCodeHandler.getSelectedElement(editor, psiFile);

        if (TestFinderHelper.findSourceElement(element) == null) {
            return;
        }

        p.setEnabled(true);
        if (TestFinderHelper.isTest(element)) {
            p.setTextValue(ActionLocalize.actionGototestsubjectText());
            p.setDescriptionValue(ActionLocalize.actionGototestsubjectDescription());
        }
        else {
            p.setTextValue(ActionLocalize.actionGototestText());
            p.setDescriptionValue(ActionLocalize.actionGototestDescription());
        }
    }
}
