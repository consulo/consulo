/*
 * Copyright 2013-2023 consulo.io
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
package consulo.web.ui.impl.internal;

import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.MenuItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2023-05-29
 */
public class WebMenuBarImpl extends VaadinComponentDelegate<WebMenuBarImpl.Vaadin> implements MenuBar {
    public static final String CLASS_NAME = "web-menu-bar";

    public class Vaadin extends com.vaadin.flow.component.menubar.MenuBar implements FromVaadinComponentWrapper {
        public Vaadin() {
            addClassName(CLASS_NAME);
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebMenuBarImpl.this;
        }
    }

    private final List<MenuItem> myItems = new ArrayList<>();

    public WebMenuBarImpl() {
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    @RequiredUIAccess
    public void clear() {
        myItems.clear();

        getVaadinComponent().removeAll();
    }

    @Override
    @RequiredUIAccess
    public MenuBar add(MenuItem menuItem) {
        myItems.add(menuItem);

        if (menuItem instanceof WebMenuItemImpl webMenuItem) {
            webMenuItem.render(getVaadinComponent());
        }

        return this;
    }

    public List<MenuItem> getItems() {
        return myItems;
    }
}
