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
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.PlainTextPresentation;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPopupMenu;

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

    private boolean myEnabled = true;

    private @Nullable String myPopupGroupId;
    private @Nullable String myPopupPlace;

    private final JPanel myTrailingPanel;
    private @Nullable Component mySuffixComponent;

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

        myTrailingPanel = new JPanel(new BorderLayout());
        myTrailingPanel.setOpaque(false);
        myTrailingPanel.add(myCloseButton, BorderLayout.CENTER);

        myTabComponent = new JPanel(new BorderLayout());
        myTabComponent.setOpaque(false);
        myTabComponent.add(myLabel, BorderLayout.CENTER);
        myTabComponent.add(myTrailingPanel, BorderLayout.EAST);

        // the tab component takes the mouse events of the tab header, so the popup is asked for here; the trigger is
        // a press on some platforms and a release on others
        MouseAdapter popupListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    showPopup(event);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    showPopup(event);
                }
            }
        };
        myTabComponent.addMouseListener(popupListener);
        myLabel.addMouseListener(popupListener);
    }

    @Override
    public void setPopupGroup(String groupId, String place) {
        myPopupGroupId = groupId;
        myPopupPlace = place;
    }

    /** The data of the group comes from the content of the tab, which is what the tab stands for. */
    private void showPopup(MouseEvent event) {
        String groupId = myPopupGroupId;
        String place = myPopupPlace;
        if (groupId == null || place == null) {
            return;
        }
        ActionManager actionManager = ActionManager.getInstance();
        if (!(actionManager.getAction(groupId) instanceof ActionGroup group)) {
            return;
        }
        ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(place, group);
        if (myComponent != null) {
            popupMenu.setTargetComponent(myComponent);
        }
        popupMenu.show(TargetAWT.wrap(event.getComponent()), event.getX(), event.getY());
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
    public LocalizeValue getTitle() {
        return PlainTextPresentation.textOf(presentation -> myRenderer.accept(this, presentation));
    }

    public Component getComponent() {
        return myComponent;
    }

    @Override
    public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {
        myCloseHandler = closeHandler;
        myCloseButton.setVisible(closeHandler != null && mySuffixComponent == null);
    }

    @Override
    @RequiredUIAccess
    public void setSuffixComponent(@Nullable Component component) {
        mySuffixComponent = component;

        myTrailingPanel.removeAll();
        if (component != null) {
            JComponent suffix = (JComponent) TargetAWT.to(component);
            suffix.setOpaque(false);
            myTrailingPanel.add(suffix, BorderLayout.CENTER);
        }
        else {
            myTrailingPanel.add(myCloseButton, BorderLayout.CENTER);
        }
        myCloseButton.setVisible(myCloseHandler != null && component == null);

        myTabComponent.revalidate();
        myTabComponent.repaint();
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    @RequiredUIAccess
    public void setEnabled(boolean enabled) {
        if (myEnabled == enabled) {
            return;
        }

        myEnabled = enabled;

        myTabbedLayout.applyEnabled(this);
    }

    @Override
    public boolean isEnabled() {
        return myEnabled;
    }

    void updateEnabledLook() {
        myLabel.setEnabled(myEnabled);
        myCloseButton.setEnabled(myEnabled);
        myTabComponent.repaint();
    }

    @Override
    public void select() {
        int index = myTabbedLayout.indexOf(this);
        if (index != -1) {
            myTabbedLayout.toAWTComponent().setSelectedIndex(index);
        }
    }
}
