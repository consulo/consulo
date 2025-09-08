/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.distributed.internal;

import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.KeepingPopupOpenAction;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;

public class BranchMoreAction extends DumbAwareAction implements KeepingPopupOpenAction {
    @Nonnull
    private final Project myProject;
    @Nullable
    private final String mySettingName;
    private final boolean myDefaultExpandValue;
    private boolean myIsExpanded;
    @Nonnull
    private final String myToCollapseText;
    @Nonnull
    private final String myToExpandText;

    public BranchMoreAction(
        @Nonnull Project project,
        int numberOfHiddenNodes,
        @Nullable String settingName,
        boolean defaultExpandValue,
        boolean hasFavorites
    ) {
        super();
        myProject = project;
        mySettingName = settingName;
        myDefaultExpandValue = defaultExpandValue;
        assert numberOfHiddenNodes > 0;
        myToExpandText = "Show " + numberOfHiddenNodes + " More...";
        myToCollapseText = "Show " + (hasFavorites ? "Only Favorites" : "Less");
        setExpanded(
            settingName != null
                ? ProjectPropertiesComponent.getInstance(project).getBoolean(settingName, defaultExpandValue)
                : defaultExpandValue
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        setExpanded(!myIsExpanded);
        InputEvent event = e.getInputEvent();
        if (event != null && event.getSource() instanceof JComponent) {
            DataProvider dataProvider = DataManager.getDataProvider((JComponent) event.getSource());
            if (dataProvider != null) {
                ObjectUtil.assertNotNull(dataProvider.getDataUnchecked(BranchListPopup.POPUP_MODEL)).refilter();
            }
        }
    }

    public boolean isExpanded() {
        return myIsExpanded;
    }

    public void setExpanded(boolean isExpanded) {
        myIsExpanded = isExpanded;
        saveState();
        updateActionText();
    }

    private void updateActionText() {
        getTemplatePresentation().setText(myIsExpanded ? myToCollapseText : myToExpandText);
    }

    public void saveState() {
        if (mySettingName != null) {
            ProjectPropertiesComponent.getInstance(myProject).setValue(mySettingName, myIsExpanded, myDefaultExpandValue);
        }
    }
}