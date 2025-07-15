/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.AnnotateToggleActionProvider;
import consulo.versionControlSystem.annotate.AnnotationProvider;
import jakarta.annotation.Nonnull;

import java.util.Set;

@ExtensionImpl
public class AnnotateVcsVirtualFileActionProvider implements AnnotateToggleActionProvider {
    @Override
    public boolean isEnabled(AnActionEvent e) {
        return AnnotateVcsVirtualFileAction.isEnabled(e);
    }

    @Override
    public boolean isSuspended(AnActionEvent e) {
        return AnnotateVcsVirtualFileAction.isSuspended(e);
    }

    @Override
    public boolean isAnnotated(AnActionEvent e) {
        return AnnotateVcsVirtualFileAction.isAnnotated(e);
    }

    @Override
    public void perform(AnActionEvent e, boolean selected) {
        AnnotateVcsVirtualFileAction.perform(e, selected);
    }

    @Nonnull
    @Override
    public LocalizeValue getActionName(@Nonnull AnActionEvent e) {
        return getActionNameImpl(e);
    }

    @Nonnull
    public static LocalizeValue getActionNameImpl(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        LocalizeValue defaultName = ActionLocalize.actionAnnotateText();
        if (project == null) return defaultName;

        Set<LocalizeValue> names = ContainerUtil.map2Set(ProjectLevelVcsManager.getInstance(project).getAllActiveVcss(), vcs -> {
            AnnotationProvider provider = vcs.getAnnotationProvider();
            if (provider != null) {
                return provider.getActionName();
            }
            return defaultName;
        });

        return ContainerUtil.getOnlyItem(names, defaultName);
    }
}
