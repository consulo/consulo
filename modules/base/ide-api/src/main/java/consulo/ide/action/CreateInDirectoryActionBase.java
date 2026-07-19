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
package consulo.ide.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.language.editor.util.IdeView;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.LegacyAnAction;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

/**
 * The base abstract class for actions which create new file elements in IDE view
 * */
public abstract class CreateInDirectoryActionBase extends AnAction implements AnActionWithAsyncUpdate {
    protected CreateInDirectoryActionBase() {
    }

    @Deprecated
    protected CreateInDirectoryActionBase(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected CreateInDirectoryActionBase(LocalizeValue text) {
        super(text);
    }

    protected CreateInDirectoryActionBase(LocalizeValue text, LocalizeValue description) {
        super(text, description);
    }

    protected CreateInDirectoryActionBase(LocalizeValue text, LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> {
            presentation.setEnabledAndVisible(isAvailable(e.getDataContext()));
        }).toCoroutine();
    }

    @Override
    public boolean isDumbAware() {
        return false;
    }

    @RequiredReadAction
    protected boolean isAvailable(DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return false;
        }

        if (DumbService.getInstance(project).isDumb() && !isDumbAware()) {
            return false;
        }

        IdeView view = dataContext.getData(IdeView.KEY);
        return view != null && view.getDirectories().length != 0;
    }
}
