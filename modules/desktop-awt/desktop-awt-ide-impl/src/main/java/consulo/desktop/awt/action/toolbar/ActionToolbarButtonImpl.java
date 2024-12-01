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
import consulo.ui.Size;
import consulo.ui.ex.action.ActionButton;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionButtonEx;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-11-21
 */
public class ActionToolbarButtonImpl extends JButton implements ActionButtonEx {
    private final ActionToolbarButtonEngine myEngine;

    public ActionToolbarButtonImpl(AnAction action, Presentation presentation, String place, @Nonnull Size minimumSize) {
        myEngine = new ActionToolbarButtonEngine(this, action, presentation, place, this::getDataContext);

        setMinimumSize(TargetAWT.to(minimumSize));

        putClientProperty(UIUtil.CENTER_TOOLTIP_DEFAULT, Boolean.TRUE);

        myEngine.updateTextAndMnemonic(presentation.getTextValue());

        addActionListener(e -> myEngine.click());
        
        myEngine.updateEnabled();
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
    
    protected DataContext getDataContext() {
        ActionToolbar actionToolbar = UIUtil.getParentOfType(ActionToolbar.class, this);
        return actionToolbar != null ? actionToolbar.getToolbarDataContext() : DataManager.getInstance().getDataContext();
    }

    @Nonnull
    @Override
    public Presentation getPresentation() {
        return myEngine.getPresentation();
    }

    @Override
    public void setIconOverrider(@Nullable Function<ActionButton, Image> imageCalculator) {

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
    public void setNoIconsInPopup(boolean value) {
        myEngine.setNoIconsInPopup(value);
    }

    @Override
    public void setCustomShortcutBuilder(Supplier<String> shortcutBuilder) {
        myEngine.setCustomShortcutBuilder(shortcutBuilder);
    }

    @Override
    public void updateIcon() {
        myEngine.updateIcon();
    }

    @Override
    public int getPopState() {
        return 0;
    }

    @Nonnull
    @Override
    public Component getUIComponent() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public AnAction getIdeAction() {
        return myEngine.getIdeAction();
    }
}
