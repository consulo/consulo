/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.MenuItem;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
class DesktopMenuItemImpl extends SwingComponentDelegate<JMenuItem> implements MenuItem {
    class MyJMenuItem extends JMenuItem implements FromSwingComponentWrapper {
        MyJMenuItem(String text) {
            super(text);
        }

        @Nonnull
        @Override
        public Component toUIComponent() {
            return DesktopMenuItemImpl.this;
        }
    }

    private final LocalizeValue myText;

    public DesktopMenuItemImpl(LocalizeValue text) {
        myText = text;
    }

    @Override
    protected JMenuItem createComponent() {
        return new MyJMenuItem(myText.get());
    }

    @Nonnull
    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        toAWTComponent().setIcon(TargetAWT.to(icon));
    }
}
