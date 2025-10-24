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
package consulo.execution.impl.internal.action.runPopup;

import consulo.execution.internal.RunConfigurationStartHistory;
import consulo.execution.localize.ExecutionLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.util.PopupUtil;
import consulo.ui.ex.internal.PopupListModelApi;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

/**
 * @author VISTALL
 * @since 2025-10-20
 */
public class AllRunConfigurationsToggle extends DumbAwareToggleAction {
    public AllRunConfigurationsToggle() {
        getTemplatePresentation().setKeepPopupOnPerform(KeepPopupOnPerform.Always);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Presentation presentation = e.getPresentation();
        presentation.setKeepPopupOnPerform(KeepPopupOnPerform.Always);

        if (project == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        boolean selected = isSelected(e);

        Toggleable.setSelected(presentation, selected);

        presentation.setTextValue(ExecutionLocalize.runToolbarWidgetAllConfigurations(10));

        presentation.setIcon(selected ? PlatformIconGroup.generalArrowdown() : PlatformIconGroup.generalArrowright());
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return false;
        }

        return RunConfigurationStartHistory.getInstance(project).isAllConfigurationsExpanded();
    }

    @RequiredUIAccess
    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        RunConfigurationStartHistory.getInstance(project).setAllConfigurationsExpanded(state);

        InputEvent inputEvent = e.getInputEvent();
        if (inputEvent != null) {
            Component component = inputEvent.getComponent();
            if (component instanceof JList list) {
                PopupListModelApi model = (PopupListModelApi) list.getModel();

                model.refilter();
                
                PopupUtil.getPopupContainerFor(list).pack(true, true);
            }
        }
    }
}
