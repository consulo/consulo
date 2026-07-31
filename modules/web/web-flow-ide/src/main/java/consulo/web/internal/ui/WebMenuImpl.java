/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.internal.ui;

import com.vaadin.flow.component.contextmenu.SubMenu;
import consulo.localize.LocalizeValue;
import consulo.ui.Menu;
import consulo.ui.MenuItem;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebMenuImpl extends WebMenuItemImpl implements Menu {
    private final List<MenuItem> myChildren = new ArrayList<>();

    private @Nullable SubMenu myVaadinSubMenu;

    public WebMenuImpl(LocalizeValue text) {
        super(text);
    }

    @Override
    @RequiredUIAccess
    public Menu add(MenuItem menuItem) {
        myChildren.add(menuItem);

        SubMenu subMenu = myVaadinSubMenu;
        if (subMenu != null && menuItem instanceof WebMenuItemImpl webMenuItem) {
            webMenuItem.render(subMenu);
        }

        return this;
    }

    public List<MenuItem> getChildren() {
        return myChildren;
    }

    @Override
    @RequiredUIAccess
    protected void renderChildren(com.vaadin.flow.component.contextmenu.MenuItem item) {
        SubMenu subMenu = item.getSubMenu();

        myVaadinSubMenu = subMenu;

        for (MenuItem child : myChildren) {
            if (child instanceof WebMenuItemImpl webMenuItem) {
                webMenuItem.render(subMenu);
            }
        }
    }
}
