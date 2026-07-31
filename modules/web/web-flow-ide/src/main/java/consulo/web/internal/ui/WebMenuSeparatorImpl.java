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

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.HasMenuItems;
import com.vaadin.flow.component.contextmenu.SubMenu;
import consulo.localize.LocalizeValue;
import consulo.ui.MenuSeparator;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebMenuSeparatorImpl extends WebMenuItemImpl implements MenuSeparator {
    public WebMenuSeparatorImpl() {
        super(LocalizeValue.empty());
    }

    @Override
    @RequiredUIAccess
    public void render(HasMenuItems target) {
        if (target instanceof SubMenu subMenu) {
            subMenu.addSeparator();
        }
        else if (target instanceof ContextMenu contextMenu) {
            contextMenu.addSeparator();
        }
    }
}
