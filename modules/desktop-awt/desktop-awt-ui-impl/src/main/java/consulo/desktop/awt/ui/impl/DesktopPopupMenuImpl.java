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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Menu;
import consulo.ui.MenuItem;
import consulo.ui.MenuSeparator;
import consulo.ui.PopupMenu;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

class DesktopPopupMenuImpl extends SwingComponentDelegate<JPopupMenu> implements PopupMenu {
    class MyJPopupMenu extends JPopupMenu implements FromSwingComponentWrapper {
        @Override
        public Component toUIComponent() {
            return DesktopPopupMenuImpl.this;
        }
    }

    private final Component myTarget;
    private boolean myOpenOnClick;

    DesktopPopupMenuImpl(Component target) {
        myTarget = target;

        if (TargetAWT.to(target) instanceof AbstractButton button) {
            button.addActionListener(event -> {
                if (myOpenOnClick) {
                    toAWTComponent().show(button, 0, button.getHeight());
                }
            });
        }
    }

    @Override
    protected JPopupMenu createComponent() {
        return new MyJPopupMenu();
    }

    @Override
    public void setOpenOnClick(boolean openOnClick) {
        myOpenOnClick = openOnClick;
    }

    @RequiredUIAccess
    @Override
    public void show(int relativeX, int relativeY) {
        java.awt.Component target = TargetAWT.to(myTarget);
        if (target == null || !target.isShowing()) {
            return;
        }
        toAWTComponent().show(target, relativeX, relativeY);
    }

    @Override
    public void hide() {
        toAWTComponent().setVisible(false);
    }

    @RequiredUIAccess
    @Override
    public Menu add(MenuItem menuItem) {
        if (menuItem instanceof MenuSeparator) {
            toAWTComponent().addSeparator();
            return this;
        }
        toAWTComponent().add((JMenuItem) TargetAWT.to(menuItem));
        return this;
    }

    @Override
    public LocalizeValue getText() {
        return LocalizeValue.empty();
    }

    @Override
    public void setIcon(@Nullable Image icon) {
    }
}
