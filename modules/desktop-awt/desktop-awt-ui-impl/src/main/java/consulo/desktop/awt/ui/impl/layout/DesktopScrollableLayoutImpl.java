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
package consulo.desktop.awt.ui.impl.layout;

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.ScrollablePanel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.ScrollableLayoutOptions;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
public class DesktopScrollableLayoutImpl extends SwingComponentDelegate<JScrollPane> implements ScrollableLayout {
    class MyJBScrollPane extends JBScrollPane implements FromSwingComponentWrapper {
        MyJBScrollPane(java.awt.Component view) {
            super(view);
        }

        @Override
        public Component toUIComponent() {
            return DesktopScrollableLayoutImpl.this;
        }
    }

    static class WidthTrackingPanel extends ScrollablePanel {
        WidthTrackingPanel(java.awt.Component view) {
            super(new BorderLayout());
            setOpaque(false);
            add(view, BorderLayout.CENTER);
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
        }
    }

    private final @Nullable Component myComponent;
    private final ScrollableLayoutOptions myOptions;

    public DesktopScrollableLayoutImpl(@Nullable Component component, ScrollableLayoutOptions options) {
        myComponent = component;
        myOptions = options;
    }

    @Override
    protected JScrollPane createComponent() {
        java.awt.Component view = TargetAWT.to(myComponent);
        if (view != null && myOptions.getHorizontalScrollPolicy() == ScrollableLayoutOptions.ScrollPolicy.NEVER) {
            view = new WidthTrackingPanel(view);
        }

        MyJBScrollPane pane = new MyJBScrollPane(view);

        pane.setBorder(IdeBorderFactory.createEmptyBorder());
        pane.setViewportBorder(IdeBorderFactory.createEmptyBorder());

        switch (myOptions.getHorizontalScrollPolicy()) {
            case ALWAYS:
                pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                break;
            case IF_NEEDED:
                pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                break;
            case NEVER:
                pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                break;
        }

        switch (myOptions.getVerticalScrollPolicy()) {
            case ALWAYS:
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                break;
            case IF_NEEDED:
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                break;
            case NEVER:
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                break;
        }

        return pane;
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        Component component = myComponent;
        if (component != null) {
            consumer.accept(component);
        }
    }
}
