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
package consulo.ide.impl.idea.codeEditor.printing;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "Print")
public class PrintAction extends AnAction implements DumbAware {
    public PrintAction() {
        super(ActionLocalize.actionPrintText(), ActionLocalize.actionPrintDescription(), PlatformIconGroup.generalPrint());
    }

    @Override
    @RequiredReadAction
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        PrintManager.executePrint(e.getDataContext());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        VirtualFile file = ReadAction.compute(() -> e.getData(VirtualFile.KEY));
        if (file != null && file.isDirectory()) {
            presentation.setEnabled(true);
            return;
        }
        presentation.setEnabled(ReadAction.compute(() -> e.hasData(PsiFile.KEY)) || e.hasData(Editor.KEY));
    }
}