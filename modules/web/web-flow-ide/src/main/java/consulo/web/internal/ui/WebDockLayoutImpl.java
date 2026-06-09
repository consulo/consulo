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
import consulo.ui.StaticPosition;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.vaadin.BorderLayoutEx;
import consulo.web.internal.ui.vaadin.VaadinSizeUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-05-27
 */
public class WebDockLayoutImpl extends WebLayoutImpl<WebDockLayoutImpl.Vaadin, StaticPosition> implements DockLayout {
    public WebDockLayoutImpl(int gapInPixels) {
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
        BorderLayoutEx.Constraint constraintEx = switch (constraint) {
            case TOP -> BorderLayoutEx.Constraint.NORTH;
            case BOTTOM -> BorderLayoutEx.Constraint.SOUTH;
            case LEFT -> BorderLayoutEx.Constraint.WEST;
            case RIGHT -> BorderLayoutEx.Constraint.EAST;
            case CENTER -> {
                VaadinSizeUtil.setSizeFull(component);

                yield BorderLayoutEx.Constraint.CENTER;
            }
            default -> throw new IllegalArgumentException(constraint.name());
        };

        return replace(component, constraintEx);
    }

    private DockLayout replace(Component child, BorderLayoutEx.Constraint constraint) {
        toVaadinComponent().setComponent(null, constraint);

        if (child != null) {
            toVaadinComponent().addComponent(TargetVaddin.to(child), constraint);
        }

        return this;
    }
}
