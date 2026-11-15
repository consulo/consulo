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
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.virtualFileSystem.VirtualFile;

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
    @SuppressWarnings("unchecked")
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        return Coroutine.first(ActionSafeReadLock.<Object, Object>run(e, p -> {
                p.setEnabledAndVisible(e.getData(Project.KEY) != null);
            }, p -> {
                // in a popup the history is for a local file, or for a context with neither a file nor PSI
                if (ActionPlaces.isPopupPlace(e.getPlace())) {
                    VirtualFile file = e.getData(VirtualFile.KEY);
                    if (file != null ? !file.isInLocalFileSystem() : e.getData(PsiElement.KEY) != null) {
                        p.setEnabledAndVisible(false);
                    }
                }
            }))
            .then(CallSubroutine.call(() -> presentation.isVisible()
                ? (Coroutine<Object, Object>) super.updateAsync(e)
                : Coroutine.<Object, Object>empty()));
    }
}

