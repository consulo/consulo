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

import com.vaadin.flow.component.orderedlayout.FlexComponent;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.StaticPosition;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.vaadin.VaadinSizeUtil;
import consulo.web.ui.impl.internal.vaadin.WebSpace;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebHorizontalLayoutImpl extends WebLayoutImpl<WebHorizontalLayoutImpl.Vaadin, StaticPosition> implements HorizontalLayout {
    public class Vaadin extends com.vaadin.flow.component.orderedlayout.HorizontalLayout implements FromVaadinComponentWrapper {
        public Vaadin() {
            // a layout adds nothing on its own, only the gap the caller asked for
            setMargin(false);
            setPadding(false);
            setSpacing(false);
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebHorizontalLayoutImpl.this;
        }
    }

    public WebHorizontalLayoutImpl(Space gap) {
        VaadinSizeUtil.setWidthFull(this);
        toVaadinComponent().setAlignItems(FlexComponent.Alignment.CENTER);

        if (gap != Space.NONE) {
            toVaadinComponent().addClassName(WebSpace.toGapClass(gap));
        }
    }

    @Override
    public void addStyle(HorizontalLayoutStyle style) {
    }

    @Override
    public HorizontalLayout add(Component component, StaticPosition constraint) {
        toVaadinComponent().add(TargetVaadin.to(component));
        return this;
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
