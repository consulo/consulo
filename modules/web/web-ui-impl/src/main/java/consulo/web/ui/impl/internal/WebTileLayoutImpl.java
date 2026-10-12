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

import com.vaadin.flow.component.html.Div;
import consulo.ui.Component;
import consulo.ui.Length;
import consulo.ui.Space;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.TileLayout;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.vaadin.VaadinSizeUtil;
import consulo.web.ui.impl.internal.vaadin.WebLength;
import consulo.web.ui.impl.internal.vaadin.WebSpace;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class WebTileLayoutImpl extends WebLayoutImpl<WebTileLayoutImpl.Vaadin, LayoutConstraint> implements TileLayout {
    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebTileLayoutImpl.this;
        }
    }

    public WebTileLayoutImpl(Length minTileWidth, Space gap) {
        VaadinSizeUtil.setWidthFull(this);

        Vaadin vaadin = toVaadinComponent();
        vaadin.addClassName("web-tile-layout");
        if (gap != Space.NONE) {
            vaadin.addClassName(WebSpace.toGapClass(gap));
        }
        vaadin.getStyle().set("grid-template-columns", "repeat(auto-fit, minmax(min(" + WebLength.toCss(minTileWidth) + ", 100%), 1fr))");
    }

    @Override
    public Layout<LayoutConstraint> add(Component component, LayoutConstraint constraint) {
        toVaadinComponent().add(TargetVaadin.to(component));
        return this;
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
