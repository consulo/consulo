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

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TabbedLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaadin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebTabbedLayoutImpl extends VaadinComponentDelegate<WebTabbedLayoutImpl.Vaadin> implements TabbedLayout {
    public class Vaadin extends TabSheet implements FromVaadinComponentWrapper {
        public Vaadin() {
            // the default border is drawn inside the sheet, so the content no longer fits and scrolls,
            // and the default padding keeps the editor from filling the panel
            addThemeVariants(TabSheetVariant.AURA_NO_BORDER, TabSheetVariant.AURA_NO_PADDING);
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebTabbedLayoutImpl.this;
        }
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public Tab createTab() {
        return new WebTabImpl(this);
    }

    @Override
    @RequiredUIAccess
    public Tab addTab(Tab tab, Component component) {
        WebTabImpl webTab = (WebTabImpl) tab;

        com.vaadin.flow.component.tabs.Tab vaadinTab = new com.vaadin.flow.component.tabs.Tab();
        // the tab is not a VaadinComponentDelegate, so it carries the small variant itself - TabVariant has no
        // constant for it, the sheet only hands its own theme down to the tabs element and not to the tabs
        vaadinTab.getElement().getThemeList().add("small");
        webTab.setVaadinTab(vaadinTab);
        webTab.update();
        com.vaadin.flow.component.Component vaadinContent = TargetVaadin.to(component);
        // the sheet does not stretch its content, without this the editor keeps its intrinsic height
        if (vaadinContent instanceof HasSize hasSize) {
            hasSize.setSizeFull();
        }

        boolean first = toVaadinComponent().getTabCount() == 0;

        toVaadinComponent().add(vaadinTab, vaadinContent);

        webTab.setContent(component);

        if (first) {
            webTab.select();
        }
        return tab;
    }

    @Override
    @RequiredUIAccess
    public Tab addTab(String tabName, Component component) {
        WebTabImpl tab = new WebTabImpl(this);
        tab.setRenderer((t, p) -> p.append(tabName));
        return addTab(tab, component);
    }

    @Override
    public void removeTab(Tab tab) {
        WebTabImpl webTab = (WebTabImpl) tab;

        com.vaadin.flow.component.tabs.Tab vaadinTab = webTab.getVaadinTab();
        if (vaadinTab != null) {
            // the sheet drops the content of the tab along with it
            toVaadinComponent().remove(vaadinTab);
        }
    }
}
