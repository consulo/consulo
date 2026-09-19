/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.editor.hierarchy;

import consulo.language.editor.internal.hierarchy.HierarchyBrowseService;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.concurrent.coroutine.Coroutine;

/**
 * Opens a hierarchy of one {@link HierarchyKind}. A language declaring a kind of its own registers an
 * action by subclassing this and naming the kind - everything else, from finding which provider serves the
 * caret to putting the result into the hierarchy tool window, is the platform's.
 * <p>
 * The action is visible only where some {@link HierarchyProvider} offers the kind, and enabled only where
 * one of them claims the current context.
 *
 * @author yole
 */
public abstract class BrowseHierarchyActionBase extends AnAction implements AnActionWithAsyncUpdate {
    private final HierarchyKind myKind;

    protected BrowseHierarchyActionBase(LocalizeValue text, LocalizeValue description, HierarchyKind kind) {
        super(text, description);
        myKind = kind;
    }

    public final HierarchyKind getKind() {
        return myKind;
    }

    @Override
    @RequiredUIAccess
    public final void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        HierarchyBrowseService.getInstance(project).browse(myKind, e.getDataContext());
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> {
            Project project = e.getData(Project.KEY);
            HierarchyBrowseService service = project == null ? null : HierarchyBrowseService.getInstance(project);

            if (service == null || !service.isSupported(myKind)) {
                presentation.setVisible(false);
            }
            else {
                boolean enabled = service.isAvailable(myKind, e.getDataContext());
                presentation.setVisible(enabled || !ActionPlaces.isPopupPlace(e.getPlace()));
                presentation.setEnabled(enabled);
            }
        }).toCoroutine();
    }
}
