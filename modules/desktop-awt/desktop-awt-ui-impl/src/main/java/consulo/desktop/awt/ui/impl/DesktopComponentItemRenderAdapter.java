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
package consulo.desktop.awt.ui.impl;

import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.ReusableComponentItemRender;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;

/**
 * Bridges a {@link ComponentItemRender} onto a swing cell renderer.
 * <p/>
 * A swing renderer is a rubber stamp, re-invoked on every paint, so a
 * {@link ReusableComponentItemRender} builds its component once and only rebinds per row.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
class DesktopComponentItemRenderAdapter<E> implements ListCellRenderer<E> {
    private final ComponentItemRender<E> myRender;

    @SuppressWarnings("rawtypes")
    private consulo.ui.Component myReusedComponent;

    DesktopComponentItemRenderAdapter(ComponentItemRender<E> render) {
        myRender = render;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
        RenderItem<E> item = RenderItem.of(value, isSelected);

        if (myRender instanceof ReusableComponentItemRender reusable) {
            if (myReusedComponent == null) {
                myReusedComponent = (consulo.ui.Component) reusable.createComponent();
            }
            reusable.bind(myReusedComponent, item);
            return TargetAWT.to(myReusedComponent);
        }

        return TargetAWT.to(myRender.render(item));
    }
}
