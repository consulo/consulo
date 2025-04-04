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
package consulo.desktop.awt.action.toolbar;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ui.Component;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2024-11-21
 */
public class ActionToggleToolbarButtonImpl extends JToggleButton implements ActionButton {
    private final ActionToolbarButtonEngine myEngine;

    public ActionToggleToolbarButtonImpl(AnAction action, Presentation presentation, String place, boolean alwaysDisplayText) {
        myEngine = new ActionToolbarButtonEngine(this, action, presentation, place, alwaysDisplayText, this::getDataContext);

        setModel(new DefaultButtonModel() {
            @Override
            public boolean isSelected() {
                return Toggleable.isSelected(presentation);
            }

            @Override
            public void setSelected(boolean b) {
            }
        });

        putClientProperty(UIUtil.CENTER_TOOLTIP_DEFAULT, Boolean.TRUE);

        myEngine.updateTextAndMnemonic(presentation.getTextValue());

        addActionListener(e -> myEngine.click());

        myEngine.updateEnabled();
    }

    protected DataContext getDataContext() {
        ActionToolbar actionToolbar = UIUtil.getParentOfType(ActionToolbar.class, this);
        return actionToolbar != null ? actionToolbar.getToolbarDataContext() : DataManager.getInstance().getDataContext();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        myEngine.addNotify();
    }

    @Override
    public void removeNotify() {
        myEngine.removeNotify();
        super.removeNotify();
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Nonnull
    @Override
    public Presentation getPresentation() {
        return myEngine.getPresentation();
    }

    @Override
    public void updateToolTipText() {
        myEngine.updateToolTipText();
    }

    @Override
    public void click() {
        myEngine.click();
    }

    @Override
    public void updateIcon() {
        myEngine.updateIcon();
    }

    @Nonnull
    @Override
    public Component getUIComponent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AnAction getIdeAction() {
        return myEngine.getIdeAction();
    }
}
