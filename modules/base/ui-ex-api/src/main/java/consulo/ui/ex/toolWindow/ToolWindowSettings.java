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
package consulo.ui.ex.toolWindow;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.ComponentManager;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ToolWindowSettings {
    static ToolWindowSettings getInstance(ComponentManager project) {
        return project.getInstance(ToolWindowSettings.class);
    }

    boolean isPaintFocus();

    void setPaintFocus(boolean value);

    ButtonDisplay getButtonDisplay();

    void setButtonDisplay(ButtonDisplay display);

    boolean isShowMnemonic();

    void setShowMnemonic(boolean value);

    boolean isHideToolStripes();

    void setHideToolStripes(boolean value);

    boolean isWidescreenSupport();

    void setWidescreenSupport(boolean value);

    boolean isLeftHorizontalSplit();

    void setLeftHorizontalSplit(boolean value);

    boolean isRightHorizontalSplit();

    void setRightHorizontalSplit(boolean value);

    boolean isAlwaysShowWindowButtons();

    void setAlwaysShowWindowButtons(boolean value);

    default boolean isPaintText() {
        ButtonDisplay display = getButtonDisplay();
        return display != ButtonDisplay.ICON && display != ButtonDisplay.LARGE_ICON;
    }

    default boolean isPaintIcon() {
        return getButtonDisplay() != ButtonDisplay.TEXT;
    }
}
