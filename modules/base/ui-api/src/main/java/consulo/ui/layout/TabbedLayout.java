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
package consulo.ui.layout;

import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public interface TabbedLayout extends Layout<LayoutConstraint> {
    static TabbedLayout create() {
        return UIInternal.get()._Layouts_tabbed();
    }

    /**
     * Create tab without adding to view
     *
     * @return new tab
     */
    Tab createTab();
    @RequiredUIAccess
    default Tab addTab(Tab tab, PseudoComponent component) {
        return addTab(tab, component.getComponent());
    }
    @RequiredUIAccess
    default Tab addTab(String tabName, PseudoComponent component) {
        return addTab(tabName, component.getComponent());
    }
    @RequiredUIAccess
    Tab addTab(Tab tab, Component component);
    @RequiredUIAccess
    Tab addTab(String tabName, Component component);

    void removeTab(Tab tab);
}
