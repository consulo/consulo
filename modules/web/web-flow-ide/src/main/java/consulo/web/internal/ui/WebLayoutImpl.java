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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.theme.lumo.LumoUtility;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.impl.BorderInfo;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.LayoutStyle;
import consulo.util.lang.ObjectUtil;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 28/05/2023
 */
public abstract class WebLayoutImpl<C extends Component & HasComponents & FromVaadinComponentWrapper, LC extends LayoutConstraint>
    extends VaadinComponentDelegate<C> implements Layout<LC> {
    @Override
    public void addStyle(LayoutStyle style) {
        // TODO
    }

    @RequiredUIAccess
    @Override
    public void removeAll() {
        toVaadinComponent().removeAll();
    }

    @Override
    public void remove(@Nonnull consulo.ui.Component component) {
        if (component.getParent() == this) {
            toVaadinComponent().remove(TargetVaddin.to(component));
        }
    }

    @Override
    public void forEachChild(@Nonnull Consumer<consulo.ui.Component> consumer) {
        C c = toVaadinComponent();

        c.getChildren()
            .map(component -> ObjectUtil.tryCast(component, FromVaadinComponentWrapper.class))
            .filter(Objects::nonNull)
            .map(FromVaadinComponentWrapper::toUIComponent)
            .forEach(consumer::accept);
    }

    @Override
    public void bordersChanged() {
        C c = toVaadinComponent();

        Map<BorderPosition, BorderInfo> borders = dataObject().getBorders();

        addBorder(c, BorderPosition.TOP, borders);
        addBorder(c, BorderPosition.BOTTOM, borders);
        addBorder(c, BorderPosition.RIGHT, borders);
        addBorder(c, BorderPosition.LEFT, borders);
    }

    private void addBorder(C c, BorderPosition pos, Map<BorderPosition, BorderInfo> borders) {
        BorderInfo info = borders.get(pos);
        if (info == null) {
            return;
        }

        switch (info.getBorderStyle()) {
            case LINE: {
                c.addClassName(LumoUtility.BorderColor.CONTRAST_10); // TODO support color
                
                switch (info.getBorderPosition()) {
                    case TOP:
                        c.addClassName(LumoUtility.Border.TOP);
                        break;
                    case BOTTOM:
                        c.addClassName(LumoUtility.Border.BOTTOM);
                        break;
                    case LEFT:
                        c.addClassName(LumoUtility.Border.LEFT);
                        break;
                    case RIGHT:
                        c.addClassName(LumoUtility.Border.RIGHT);
                        break;
                }
                break;
            }
            case EMPTY: {
                switch (info.getBorderPosition()) {
                    case TOP:
                        c.addClassName(LumoUtility.Padding.Top.SMALL);
                        break;
                    case BOTTOM:
                        c.addClassName(LumoUtility.Padding.Bottom.SMALL);
                        break;
                    case LEFT:
                        c.addClassName(LumoUtility.Padding.Left.SMALL);
                        break;
                    case RIGHT:
                        c.addClassName(LumoUtility.Padding.Right.SMALL);
                        break;
                }
                break;
            }
        }
    }
}
