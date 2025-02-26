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
package consulo.desktop.awt.action.menu;

import consulo.dataContext.DataContext;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2024-11-24
 */
public class ActionMenuItemImpl extends JMenuItem implements ActionMenuItem {
    private ActionMenuItemEngine myEngine;

    public ActionMenuItemImpl(AnAction action,
                              Presentation presentation,
                              @Nonnull String place,
                              @Nonnull DataContext context,
                              boolean enableMnemonics,
                              boolean prepareNow,
                              boolean insideCheckedGroup,
                              boolean enableIcons) {
        myEngine = new ActionMenuItemEngine(this, action, presentation, place, context, enableMnemonics, prepareNow, insideCheckedGroup, enableIcons);
    }

    @Override
    public void prepare() {
        myEngine.prepare();
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
}
