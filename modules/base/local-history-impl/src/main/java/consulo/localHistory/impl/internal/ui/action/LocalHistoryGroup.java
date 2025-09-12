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
package consulo.localHistory.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.language.psi.PsiElement;
import consulo.localHistory.localize.LocalHistoryLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.NonTrivialActionGroup;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

@ActionImpl(
    id = "LocalHistory",
    children = {
        @ActionRef(type = ShowHistoryAction.class),
        @ActionRef(type = ShowSelectionHistoryAction.class),
        @ActionRef(type = PutLabelAction.class)
    },
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_VERSION_CONTROLS), anchor = ActionRefAnchor.FIRST)
)
public class LocalHistoryGroup extends NonTrivialActionGroup implements DumbAware {
    public LocalHistoryGroup() {
      super(LocalHistoryLocalize.groupLocalHistoryText(), true);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        VirtualFile file = e.getData(VirtualFile.KEY);
        PsiElement element = e.getData(PsiElement.KEY);
        if (project == null || ActionPlaces.isPopupPlace(e.getPlace())
            && (file != null && !file.isInLocalFileSystem() || file == null && element != null)) {
            e.getPresentation().setEnabledAndVisible(false);
        }
        else {
            super.update(e);
        }
    }
}

