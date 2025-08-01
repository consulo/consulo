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
package consulo.ide.impl.idea.openapi.wm.impl;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.util.UserHomeFileUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Bas Leijdekkers
 */
@ActionImpl(
    id = "OpenProjectWindows",
    children = {
        @ActionRef(type = NextProjectWindow.class),
        @ActionRef(type = PreviousProjectWindow.class),
        @ActionRef(type = AnSeparator.class)
    }
)
public class ProjectWindowActionGroup extends DefaultActionGroup {
    private ProjectWindowAction latest = null;

    public void addProject(@Nonnull Project project) {
        String projectLocation = project.getPresentableUrl();
        if (projectLocation == null) {
            return;
        }
        String projectName = project.getName();
        ProjectWindowAction windowAction = new ProjectWindowAction(projectName, projectLocation, latest);
        List<ProjectWindowAction> duplicateWindowActions = findWindowActionsWithProjectName(projectName);
        if (!duplicateWindowActions.isEmpty()) {
            for (ProjectWindowAction action : duplicateWindowActions) {
                action.getTemplatePresentation().setText(UserHomeFileUtil.getLocationRelativeToUserHome(action.getProjectLocation()));
            }
            windowAction.getTemplatePresentation()
                .setText(UserHomeFileUtil.getLocationRelativeToUserHome(windowAction.getProjectLocation()));
        }
        add(windowAction);
        latest = windowAction;
    }

    public void removeProject(@Nonnull Project project) {
        ProjectWindowAction windowAction = findWindowAction(project.getPresentableUrl());
        if (windowAction == null) {
            return;
        }
        if (latest == windowAction) {
            ProjectWindowAction previous = latest.getPrevious();
            latest = previous != latest ? previous : null;
        }
        remove(windowAction);
        String projectName = project.getName();
        List<ProjectWindowAction> duplicateWindowActions = findWindowActionsWithProjectName(projectName);
        if (duplicateWindowActions.size() == 1) {
            duplicateWindowActions.get(0).getTemplatePresentation().setText(projectName);
        }
        windowAction.dispose();
    }

    public boolean isEnabled() {
        return latest != null && latest.getPrevious() != latest;
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }

    @RequiredUIAccess
    public void activateNextWindow(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        ProjectWindowAction windowAction = findWindowAction(project.getPresentableUrl());
        if (windowAction == null) {
            return;
        }
        ProjectWindowAction next = windowAction.getNext();
        if (next != null) {
            next.setSelected(e, true);
        }
    }

    @RequiredUIAccess
    public void activatePreviousWindow(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        ProjectWindowAction windowAction = findWindowAction(project.getPresentableUrl());
        if (windowAction == null) {
            return;
        }
        ProjectWindowAction previous = windowAction.getPrevious();
        if (previous != null) {
            previous.setSelected(e, true);
        }
    }

    @Nullable
    private ProjectWindowAction findWindowAction(String projectLocation) {
        if (projectLocation == null) {
            return null;
        }
        AnAction[] children = getChildren(null);
        for (AnAction child : children) {
            if (child instanceof ProjectWindowAction windowAction && projectLocation.equals(windowAction.getProjectLocation())) {
                return windowAction;
            }
        }
        return null;
    }

    private List<ProjectWindowAction> findWindowActionsWithProjectName(String projectName) {
        List<ProjectWindowAction> result = null;
        AnAction[] children = getChildren(null);
        for (AnAction child : children) {
            if (child instanceof ProjectWindowAction windowAction && projectName.equals(windowAction.getProjectName())) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(windowAction);
            }
        }
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }
}