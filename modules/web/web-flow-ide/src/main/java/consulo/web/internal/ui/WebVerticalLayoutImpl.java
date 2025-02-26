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
package consulo.web.internal.ui;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.VerticalLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 27/05/2023
 */
public class WebVerticalLayoutImpl extends VaadinComponentDelegate<WebVerticalLayoutImpl.Vaadin> implements VerticalLayout {
    public class Vaadin extends com.vaadin.flow.component.orderedlayout.VerticalLayout implements FromVaadinComponentWrapper {
        @Nullable
        @Override
        public Component toUIComponent() {
            return WebVerticalLayoutImpl.this;
        }
    }

    @Override
    public void addStyle(LayoutStyle style) {
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public VerticalLayout add(@Nonnull Component component) {
        toVaadinComponent().add(TargetVaddin.to(component));
        return this;
    }

    @Nonnull
    @Override
    public WebVerticalLayoutImpl.Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
