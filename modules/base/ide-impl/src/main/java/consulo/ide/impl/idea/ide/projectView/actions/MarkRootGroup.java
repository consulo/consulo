/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.projectView.actions;

import consulo.module.Module;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.util.ActionGroupUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class MarkRootGroup extends ActionGroup implements DumbAware {
    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setVisible(!ActionGroupUtil.isGroupEmpty(this, e));
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return EMPTY_ARRAY;
        }
        Module module = e.getData(Module.KEY);
        if (module == null) {
            return EMPTY_ARRAY;
        }

        List<AnAction> actionList = new ArrayList<>(5);
        //for (ContentFolderType contentFolderType : ContentFolderType.ALL_SOURCE_ROOTS) {
        //    actionList.add(new MarkRootAction(contentFolderType));
        //}
        //actionList.add(new MarkExcludeRootAction());
        //if (!actionList.isEmpty()) {
        //    actionList.add(AnSeparator.getInstance());
        //    actionList.add(new UnmarkRootAction());
        //}
        return actionList.isEmpty() ? AnAction.EMPTY_ARRAY : actionList.toArray(new AnAction[actionList.size()]);
    }
}
