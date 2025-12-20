/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.awt.ui.impl.layout;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.WrappedLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 25-Oct-17
 */
public class DesktopWrappedLayoutImpl extends SwingComponentDelegate<JPanel> implements WrappedLayout {
    class MyJPanel extends JPanel implements FromSwingComponentWrapper {
        MyJPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public void requestFocus() {
            if (getTargetComponent() == this) {
                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(super::requestFocus);
                return;
            }
            IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(getTargetComponent());
        }

        @Override
        public boolean requestFocusInWindow() {
            if (getTargetComponent() == this) {
                return super.requestFocusInWindow();
            }
            return getTargetComponent().requestFocusInWindow();
        }

        @Override
        public final boolean requestFocus(boolean temporary) {
            if (getTargetComponent() == this) {
                return super.requestFocus(temporary);
            }
            return getTargetComponent().requestFocus(temporary);
        }

        @Nonnull
        @Override
        public Component toUIComponent() {
            return DesktopWrappedLayoutImpl.this;
        }
    }

    public DesktopWrappedLayoutImpl() {

    }

    @Override
    protected JPanel createComponent() {
        JPanel panel = new MyJPanel(new BorderLayout());
        panel.setOpaque(false);
        return panel;
    }

    @Override
    public void addStyle(LayoutStyle style) {
        DesktopAWTLayoutStyleHandler.addStyle(style, toAWTComponent());
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public WrappedLayout set(@Nullable Component component) {
        setContent(component == null ? null : (JComponent) TargetAWT.to(component));
        return this;
    }

    public void setContent(JComponent wrapped) {
        if (wrapped == getTargetComponent()) {
            return;
        }

        JPanel component = toAWTComponent();
        component.removeAll();
        component.setLayout(new BorderLayout());
        if (wrapped != null) {
            component.add(wrapped, BorderLayout.CENTER);
        }
        component.validate();
    }

    public JComponent getTargetComponent() {
        JPanel component = toAWTComponent();
        if (component.getComponentCount() == 1) {
            return (JComponent) component.getComponent(0);
        }
        else {
            return component;
        }
    }
}
