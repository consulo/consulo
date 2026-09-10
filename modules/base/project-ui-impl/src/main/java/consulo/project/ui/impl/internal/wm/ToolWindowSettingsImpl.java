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
package consulo.project.ui.impl.internal.wm;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ui.ex.toolWindow.ButtonDisplay;
import consulo.ui.ex.toolWindow.ToolWindowSettings;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
@Singleton
@State(name = "ToolWindowSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public class ToolWindowSettingsImpl implements ToolWindowSettings, PersistentStateComponent<ToolWindowSettingsState> {
    private ToolWindowSettingsState myState = new ToolWindowSettingsState();

    @Override
    public boolean isPaintFocus() {
        return myState.paintFocus;
    }

    @Override
    public void setPaintFocus(boolean value) {
        myState.paintFocus = value;
    }

    @Override
    public boolean isShowMnemonic() {
        return myState.showMnemonic;
    }

    @Override
    public void setShowMnemonic(boolean value) {
        myState.showMnemonic = value;
    }

    @Override
    public boolean isHideToolStripes() {
        return myState.hideToolStripes;
    }

    @Override
    public void setHideToolStripes(boolean value) {
        myState.hideToolStripes = value;
    }

    @Override
    public boolean isWidescreenSupport() {
        return myState.widescreenSupport;
    }

    @Override
    public void setWidescreenSupport(boolean value) {
        myState.widescreenSupport = value;
    }

    @Override
    public boolean isLeftHorizontalSplit() {
        return myState.leftHorizontalSplit;
    }

    @Override
    public void setLeftHorizontalSplit(boolean value) {
        myState.leftHorizontalSplit = value;
    }

    @Override
    public boolean isRightHorizontalSplit() {
        return myState.rightHorizontalSplit;
    }

    @Override
    public void setRightHorizontalSplit(boolean value) {
        myState.rightHorizontalSplit = value;
    }

    @Override
    public boolean isAlwaysShowWindowButtons() {
        return myState.alwaysShowWindowButtons;
    }

    @Override
    public void setAlwaysShowWindowButtons(boolean value) {
        myState.alwaysShowWindowButtons = value;
    }

    @Override
    public ButtonDisplay getButtonDisplay() {
        return myState.buttonDisplay;
    }

    @Override
    public void setButtonDisplay(ButtonDisplay display) {
        myState.buttonDisplay = display;
    }

    @Override
    public @Nullable ToolWindowSettingsState getState() {
        return myState;
    }

    @Override
    public void loadState(ToolWindowSettingsState state) {
        myState = state;
    }
}
