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
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.copy.CopyHandler;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.Presentation;

@ActionImpl(id = "CloneElement")
public class CloneElementAction extends CopyElementAction {
    private static final Logger LOG = Logger.getInstance(CloneElementAction.class);

    public CloneElementAction() {
        super(ActionLocalize.actionCloneelementText(), ActionLocalize.actionCloneelementDescription());
    }

    @Override
    protected void doCopy(PsiElement[] elements, PsiDirectory defaultTargetDirectory) {
        LOG.assertTrue(elements.length == 1);
        CopyHandler.doClone(elements[0]);
    }

    @Override
    @RequiredUIAccess
    protected void updateForEditor(DataContext dataContext, Presentation presentation) {
        super.updateForEditor(dataContext, presentation);
        presentation.setVisible(false);
    }

    @Override
    protected void updateForToolWindow(String id, DataContext dataContext, Presentation presentation) {
        // work only with single selection
        PsiElement[] elements = dataContext.getData(PsiElement.KEY_OF_ARRAY);
        presentation.setEnabled(elements != null && elements.length == 1 && CopyHandler.canClone(elements));
        presentation.setVisible(true);
        if (!ToolWindowId.COMMANDER.equals(id)) {
            presentation.setVisible(false);
        }
    }
}
