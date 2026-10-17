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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class GroupByDirectoryToggleAction extends DumbAwareToggleAction {
    private final BooleanSupplier myGrouped;
    private final Consumer<Boolean> mySetGrouped;

    public GroupByDirectoryToggleAction(BooleanSupplier grouped, Consumer<Boolean> setGrouped) {
        super(
            VcsLocalize.changesActionShowDirectoriesText(),
            VcsLocalize.changesActionShowDirectoriesDescription(),
            PlatformIconGroup.actionsGroupbypackage()
        );
        myGrouped = grouped;
        mySetGrouped = setGrouped;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return myGrouped.getAsBoolean();
    }

    @RequiredUIAccess
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        mySetGrouped.accept(state);
    }
}
