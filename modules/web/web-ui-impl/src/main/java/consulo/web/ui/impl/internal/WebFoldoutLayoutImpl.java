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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.details.Details;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.layout.event.FoldoutLayoutOpenedEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
public class WebFoldoutLayoutImpl extends VaadinComponentDelegate<WebFoldoutLayoutImpl.Vaadin> implements FoldoutLayout {
    public class Vaadin extends Details implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebFoldoutLayoutImpl.this;
        }
    }

    private LocalizeValue myTitleValue;
    private final Component myComponent;

    @SuppressWarnings("unchecked")
    public WebFoldoutLayoutImpl(LocalizeValue titleValue, Component component, boolean state) {
        myTitleValue = titleValue;
        myComponent = component;

        Vaadin vaadin = getVaadinComponent();
        vaadin.setSummaryText(titleValue.get());
        vaadin.add(TargetVaadin.to(component));
        vaadin.setOpened(state);

        vaadin.addOpenedChangeListener(event -> getListenerDispatcher(FoldoutLayoutOpenedEvent.class)
            .onEvent(new FoldoutLayoutOpenedEvent(this, event.isOpened())));
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @RequiredUIAccess
    @Override
    public FoldoutLayout setState(boolean showing) {
        getVaadinComponent().setOpened(showing);
        return this;
    }

    @RequiredUIAccess
    @Override
    public FoldoutLayout setTitle(LocalizeValue title) {
        myTitleValue = title;
        getVaadinComponent().setSummaryText(title.get());
        return this;
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        consumer.accept(myComponent);
    }
}
