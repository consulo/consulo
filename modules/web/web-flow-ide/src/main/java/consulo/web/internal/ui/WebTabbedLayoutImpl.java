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

import com.vaadin.flow.component.tabs.TabSheet;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.TabbedLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebTabbedLayoutImpl extends VaadinComponentDelegate<WebTabbedLayoutImpl.Vaadin> implements TabbedLayout {
    public class Vaadin extends TabSheet implements FromVaadinComponentWrapper {

        @Nullable
        @Override
        public Component toUIComponent() {
            return WebTabbedLayoutImpl.this;
        }
    }

    @Override
    @Nonnull
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Nonnull
    @Override
    public Tab createTab() {
        return new WebTabImpl(this);
    }

    @Override
    public void addStyle(LayoutStyle style) {
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Tab addTab(@Nonnull Tab tab, @Nonnull Component component) {
        WebTabImpl webTab = (WebTabImpl) tab;

        com.vaadin.flow.component.tabs.Tab vaadinTab = new com.vaadin.flow.component.tabs.Tab();
        webTab.setVaadinTab(vaadinTab);
        webTab.update();
        toVaadinComponent().add(vaadinTab, TargetVaddin.to(component));
        return tab;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Tab addTab(@Nonnull String tabName, @Nonnull Component component) {
        WebTabImpl tab = new WebTabImpl(this);
        tab.setRender((t, p) -> p.append(tabName));
        return addTab(tab, component);
    }

    @Override
    public void removeTab(@Nonnull Tab tab) {
        // todo
    }
}
