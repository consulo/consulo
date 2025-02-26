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

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LayoutStyle;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebHorizontalLayoutImpl extends VaadinComponentDelegate<WebHorizontalLayoutImpl.Vaadin> implements HorizontalLayout {
    public class Vaadin extends com.vaadin.flow.component.orderedlayout.HorizontalLayout implements FromVaadinComponentWrapper {

        @Nullable
        @Override
        public Component toUIComponent() {
            return WebHorizontalLayoutImpl.this;
        }
    }

    @Override
    public void addStyle(LayoutStyle style) {
    }

    @Override
    @Nonnull
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public HorizontalLayout add(@Nonnull consulo.ui.Component component) {
        getVaadinComponent().add(TargetVaddin.to(component));
        return this;
    }
}
