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
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.WrappedLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.SingleComponentLayout;
import consulo.web.internal.ui.vaadin.VaadinSizeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WebWrappedLayoutImpl extends VaadinComponentDelegate<WebWrappedLayoutImpl.Vaadin> implements WrappedLayout {
    public class Vaadin extends SingleComponentLayout implements FromVaadinComponentWrapper {
        @Nullable
        @Override
        public Component toUIComponent() {
            return WebWrappedLayoutImpl.this;
        }
    }

    @Nonnull
    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public void addStyle(LayoutStyle style) {
    }

    @RequiredUIAccess
    @Override
    public void removeAll() {
        getVaadinComponent().setContent(null);
    }

    @Override
    public void remove(@Nonnull Component component) {
        getVaadinComponent().removeIfContent(TargetVaddin.to(component));
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public WrappedLayout set(@Nullable Component component) {
        if (component != null) {
            VaadinSizeUtil.setSizeFull(component);
        }
        getVaadinComponent().setContent(TargetVaddin.to(component));
        return this;
    }
}
