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
package consulo.desktop.awt.action;

import consulo.application.dumb.DumbAware;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.image.Image;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-12-01
 */
public class SecondaryActionGroup extends DefaultActionGroup implements DumbAware {
    private boolean myShowArrowBelow;

    @Override
    public boolean showBelowArrow() {
        return myShowArrowBelow;
    }

    public void setShowArrowBelow(boolean showArrowBelow) {
        myShowArrowBelow = showArrowBelow;
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.generalGearplain();
    }

    @Override
    public boolean isPopup() {
        return true;
    }
}
