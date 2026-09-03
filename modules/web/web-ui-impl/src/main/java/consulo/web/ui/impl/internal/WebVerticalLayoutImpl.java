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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Space;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.VerticalLayout;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.vaadin.VaadinSizeUtil;
import consulo.web.ui.impl.internal.vaadin.WebSpace;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-05-27
 */
public class WebVerticalLayoutImpl extends VaadinComponentDelegate<WebVerticalLayoutImpl.Vaadin> implements VerticalLayout {
    public class Vaadin extends com.vaadin.flow.component.orderedlayout.VerticalLayout implements FromVaadinComponentWrapper {
        public Vaadin() {
            // a layout adds nothing on its own, only the gap the caller asked for
            setMargin(false);
            setPadding(false);
            setSpacing(false);
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebVerticalLayoutImpl.this;
        }
    }

    public WebVerticalLayoutImpl(Space vGap) {
        this(vGap, null);
    }

    public WebVerticalLayoutImpl(Space vGap, @Nullable HorizontalAlignment alignment) {
        VaadinSizeUtil.setWidthFull(this);

        if (vGap != Space.NONE) {
            toVaadinComponent().addClassName(WebSpace.toGapClass(vGap));
        }

        if (alignment != null) {
            toVaadinComponent().setAlignItems(switch (alignment) {
                case LEFT -> Alignment.START;
                case CENTER -> Alignment.CENTER;
                case RIGHT -> Alignment.END;
            });
        }
    }

    @Override
    public Layout<LayoutConstraint> add(Component component, LayoutConstraint constraint) {
        toVaadinComponent().add(TargetVaadin.to(component));
        return this;
    }

    @Override
    public WebVerticalLayoutImpl.Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
