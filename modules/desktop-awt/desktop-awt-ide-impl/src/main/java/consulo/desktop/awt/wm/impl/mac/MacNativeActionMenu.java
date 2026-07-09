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
package consulo.desktop.awt.wm.impl.mac;

import consulo.application.ui.UISettings;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.mac.screenmenu.Menu;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.PresentationFactory;
import consulo.ui.ex.action.Toggleable;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 * Builds a native {@link Menu} peer (submenu) for an {@link ActionGroup}. The submenu is filled lazily and
 * asynchronously when it is opened.
 *
 * @author VISTALL
 */
public final class MacNativeActionMenu {
    private MacNativeActionMenu() {
    }

    public static Menu create(DataContext context,
                              String place,
                              ActionGroup group,
                              PresentationFactory presentationFactory,
                              @Nullable Component contextComponent) {
        Presentation presentation = presentationFactory.getPresentation(group);
        Menu menu = new Menu(MacScreenMenuFiller.menuText(presentation));

        if (group instanceof Toggleable && Toggleable.isSelected(presentation)) {
            menu.setState(true);
        }

        menu.setOnOpen(contextComponent, () -> {
            DataContext freshContext = contextComponent != null
                ? DataManager.getInstance().getDataContext(contextComponent)
                : context;
            MacScreenMenuFiller.fill(menu, group, freshContext, place, presentationFactory, contextComponent);
        }, true);

        menu.listenPresentationChanges(presentation);

        if (MacScreenMenuFiller.SHOW_ICONS && UISettings.getInstance().SHOW_ICONS_IN_MENUS) {
            Image icon = presentation.getIcon();
            if (icon != null) {
                menu.setIcon(TargetAWT.to(icon));
            }
        }

        return menu;
    }
}
