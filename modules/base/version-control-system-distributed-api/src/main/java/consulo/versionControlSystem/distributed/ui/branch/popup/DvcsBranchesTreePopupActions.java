/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.distributed.ui.branch.popup;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.ToggleAction;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public final class DvcsBranchesTreePopupActions {
    private DvcsBranchesTreePopupActions() {
    }

    public static ActionGroup createHeaderGroup(
        DvcsBranchesTreePopupModel<?> model,
        DvcsBranchesTreePopupStepBase step,
        Runnable afterGroupingChanged
    ) {
        DefaultActionGroup group = new DefaultActionGroup();
        model.appendHeaderActions(group::add);
        group.add(new SettingsActionGroup(step, afterGroupingChanged));
        return group;
    }

    private static class SettingsActionGroup extends DefaultActionGroup {
        private SettingsActionGroup(DvcsBranchesTreePopupStepBase step, Runnable afterGroupingChanged) {
            super("Settings", true);
            getTemplatePresentation().setIcon(PlatformIconGroup.generalGearplain());
            add(new DirectoryGroupingToggleAction(step, afterGroupingChanged));
        }

        @Override
        public boolean showBelowArrow() {
            return false;
        }
    }

    private static class DirectoryGroupingToggleAction extends ToggleAction {
        private final DvcsBranchesTreePopupStepBase myStep;
        private final Runnable myAfterGroupingChanged;

        private DirectoryGroupingToggleAction(DvcsBranchesTreePopupStepBase step, Runnable afterGroupingChanged) {
            super("Group Branches by Directory");
            myStep = step;
            myAfterGroupingChanged = afterGroupingChanged;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myStep.getTreeModel().isDirectoryGrouping();
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent e, boolean state) {
            myStep.getTreeModel().setDirectoryGrouping(state);
            myAfterGroupingChanged.run();
        }
    }
}
