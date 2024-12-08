/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.debug.impl.internal.memory.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.execution.debug.XDebuggerBundle;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.image.Image;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-12-08
 */
@ActionImpl(id = "MemoryView.SettingsPopupActionGroup", children = {
    @ActionRef(type = ShowClassesWithInstanceAction.class),
    @ActionRef(type = ShowClassesWithDiffAction.class),
    @ActionRef(type = ShowTrackedAction.class),
    @ActionRef(type = AnSeparator.class),
    @ActionRef(type = EnableBackgroundTrackingAction.class),
    @ActionRef(type = SwitchUpdateModeAction.class)
})
public class MemorySettingsPopupActionGroup extends DefaultActionGroup implements DumbAware {
    public MemorySettingsPopupActionGroup() {
        super(XDebuggerBundle.message("action.memory.view.settings.text"), true);
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.generalGearplain();
    }

    @Override
    public boolean showBelowArrow() {
        return false;
    }

    @Override
    public boolean isPopup() {
        return true;
    }
}
