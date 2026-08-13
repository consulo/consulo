/*
 * Copyright 2013-2017 consulo.io
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

import consulo.desktop.awt.ui.impl.DesktopTextItemPresentationImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.TextItemPresentation;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SimpleColoredComponent;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2017-09-12
 */
public class DesktopTabImpl implements Tab {
    private final DesktopTabbedLayoutImpl myTabbedLayout;

    private final JPanel myTabComponent;
    private final SimpleColoredComponent myLabel;
    private final JBLabel myCloseButton;

    private BiConsumer<Tab, TextItemPresentation> myRenderer = (tab, presentation) -> presentation.append(toString());
    private @Nullable BiConsumer<Tab, Component> myCloseHandler;

    private Component myComponent;

    public DesktopTabImpl(DesktopTabbedLayoutImpl tabbedLayout) {
        myTabbedLayout = tabbedLayout;

        myLabel = new SimpleColoredComponent();
        myLabel.setOpaque(false);

        myCloseButton = new JBLabel(PlatformIconGroup.actionsCancel());
        myCloseButton.setBorder(JBUI.Borders.emptyLeft(4));
        myCloseButton.setVisible(false);
        myCloseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                BiConsumer<Tab, Component> closeHandler = myCloseHandler;
                if (closeHandler != null) {
                    closeHandler.accept(DesktopTabImpl.this, myComponent);
                }
            }
        });

        myTabComponent = new JPanel(new BorderLayout());
        myTabComponent.setOpaque(false);
        myTabComponent.add(myLabel, BorderLayout.CENTER);
        myTabComponent.add(myCloseButton, BorderLayout.EAST);
    }

    public void setComponent(Component component) {
        myComponent = component;
    }

    public JComponent getTabComponent() {
        return myTabComponent;
    }

    @Override
    public void update() {
        TextItemPresentation presentation = new DesktopTextItemPresentationImpl(myLabel);
        presentation.clearText();
        myRenderer.accept(this, presentation);

        myTabComponent.revalidate();
        myTabComponent.repaint();
    }

    @Override
    public void setRenderer(BiConsumer<Tab, TextItemPresentation> renderer) {
        myRenderer = renderer;
    }

    @Override
    public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {
        myCloseHandler = closeHandler;
        myCloseButton.setVisible(closeHandler != null);
    }

    @Override
    public void select() {
        int index = myTabbedLayout.indexOf(this);
        if (index != -1) {
            myTabbedLayout.toAWTComponent().setSelectedIndex(index);
        }
    }
}
