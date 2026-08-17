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
package consulo.web.ui.impl.internal;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.WrappedLayout;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.vaadin.SingleComponentLayout;
import consulo.web.ui.impl.internal.vaadin.VaadinSizeUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WebWrappedLayoutImpl extends VaadinComponentDelegate<WebWrappedLayoutImpl.Vaadin> implements WrappedLayout {
    public class Vaadin extends SingleComponentLayout implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebWrappedLayoutImpl.this;
        }
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        getVaadinComponent().setContent(null);
    }

    @Override
    public void remove(Component component) {
        getVaadinComponent().removeIfContent(TargetVaadin.to(component));
    }

    @Override
    @RequiredUIAccess
    public WrappedLayout set(@Nullable Component component) {
        if (component != null) {
            VaadinSizeUtil.setSizeFull(component);
        }
        getVaadinComponent().setContent(TargetVaadin.to(component));
        return this;
    }
}
