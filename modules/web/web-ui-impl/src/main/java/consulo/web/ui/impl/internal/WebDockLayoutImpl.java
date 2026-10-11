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

import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.StaticPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.LayoutVisualOrder;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.vaadin.BorderLayoutEx;
import consulo.web.ui.impl.internal.vaadin.VaadinSizeUtil;
import consulo.web.ui.impl.internal.vaadin.WebSpace;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2023-05-27
 */
public class WebDockLayoutImpl extends WebLayoutImpl<WebDockLayoutImpl.Vaadin, StaticPosition> implements DockLayout {
    public WebDockLayoutImpl(Space gap) {
        if (gap != Space.NONE) {
            toVaadinComponent().setGapClass(WebSpace.toGapClass(gap));
        }
    }

    public class Vaadin extends BorderLayoutEx implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebDockLayoutImpl.this;
        }
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public Layout<StaticPosition> add(Component component, StaticPosition constraint) {
        if (constraint == StaticPosition.CENTER) {
            VaadinSizeUtil.setSizeFull(component);
        }

        return replace(component, toConstraint(constraint));
    }

    private static BorderLayoutEx.Constraint toConstraint(StaticPosition position) {
        return switch (position) {
            case TOP -> BorderLayoutEx.Constraint.NORTH;
            case BOTTOM -> BorderLayoutEx.Constraint.SOUTH;
            case LEFT -> BorderLayoutEx.Constraint.WEST;
            case RIGHT -> BorderLayoutEx.Constraint.EAST;
            case CENTER -> BorderLayoutEx.Constraint.CENTER;
        };
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        Vaadin vaadin = toVaadinComponent();
        for (StaticPosition position : LayoutVisualOrder.DOCK_POSITIONS) {
            WebLayoutChildren.accept(vaadin.getComponent(toConstraint(position)), consumer);
        }
    }

    @Override
    public void remove(Component component) {
        toVaadinComponent().removeComponent(TargetVaadin.to(component));
    }

    private DockLayout replace(Component child, BorderLayoutEx.Constraint constraint) {
        toVaadinComponent().setComponent(null, constraint);

        if (child != null) {
            toVaadinComponent().addComponent(TargetVaadin.to(child), constraint);
        }

        return this;
    }
}
