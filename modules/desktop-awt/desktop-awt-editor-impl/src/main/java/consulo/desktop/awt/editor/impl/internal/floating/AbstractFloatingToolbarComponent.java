// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.desktop.awt.editor.impl.internal.floating;

import consulo.codeEditor.impl.internal.floating.TransparentComponent;
import consulo.codeEditor.impl.internal.floating.TransparentComponentAnimator;
import consulo.codeEditor.toolbar.floating.FloatingToolbarComponent;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

public abstract class AbstractFloatingToolbarComponent extends JPanel implements FloatingToolbarComponent {
    private static final Color BACKGROUND = JBColor.namedColor("Toolbar.Floating.background", 0xEDEDED, 0x454A4D);

    private static final float BACKGROUND_ALPHA = 0.55f;

    private final ActionToolbar myToolbar;

    private final ToolbarTransparentComponent myTransparentComponent = new ToolbarTransparentComponent();

    private final TransparentComponentAnimator myComponentAnimator;

    @RequiredUIAccess
    protected AbstractFloatingToolbarComponent(ActionGroup actionGroup, JComponent ownerComponent, Disposable parentDisposable) {
        super(new BorderLayout());

        myToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CONTEXT_TOOLBAR, actionGroup, true);
        myToolbar.setTargetComponent(ownerComponent);

        JComponent toolbarComponent = myToolbar.getComponent();
        toolbarComponent.setOpaque(false);
        toolbarComponent.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                myTransparentComponent.fireActionsUpdated();
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                myTransparentComponent.fireActionsUpdated();
            }
        });
        add(toolbarComponent, BorderLayout.CENTER);
        setOpaque(false);

        myComponentAnimator = new TransparentComponentAnimator(myTransparentComponent, parentDisposable);
    }

    protected boolean isComponentOnHold() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void addNotify() {
        super.addNotify();
        myToolbar.updateActionsImmediately();
    }

    @Override
    public boolean isAutoHideable() {
        return myComponentAnimator.isAutoHideable();
    }

    @Override
    @RequiredUIAccess
    public void setAutoHideable(boolean autoHideable) {
        myComponentAnimator.setAutoHideable(autoHideable);
    }

    @Override
    @RequiredUIAccess
    public void scheduleShow() {
        myComponentAnimator.scheduleShow();
    }

    @Override
    @RequiredUIAccess
    public void scheduleHide() {
        myComponentAnimator.scheduleHide();
    }

    @Override
    @RequiredUIAccess
    public void hideImmediately() {
        myComponentAnimator.hideImmediately();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics graphics = g.create();
        try {
            if (graphics instanceof Graphics2D graphics2D) {
                float opacity = myTransparentComponent.getOpacity() * BACKGROUND_ALPHA;
                graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
            graphics.setColor(BACKGROUND);
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

            super.paintComponent(graphics);
        }
        finally {
            graphics.dispose();
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics graphics = g.create();
        try {
            if (graphics instanceof Graphics2D graphics2D) {
                float opacity = myTransparentComponent.getOpacity();
                graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            }
            super.paintChildren(graphics);
        }
        finally {
            graphics.dispose();
        }
    }

    private boolean hasVisibleActions() {
        return myToolbar.getComponent().getComponentCount() > 0;
    }

    private final class ToolbarTransparentComponent implements TransparentComponent {
        private boolean myVisible = false;

        private float myOpacity = 0.0f;

        float getOpacity() {
            return myOpacity;
        }

        @Override
        public void setOpacity(float opacity) {
            myOpacity = opacity;
        }

        @Override
        public boolean isComponentOnHold() {
            return AbstractFloatingToolbarComponent.this.isComponentOnHold();
        }

        @Override
        @RequiredUIAccess
        public void showComponent() {
            myVisible = true;
            myToolbar.updateActionsImmediately();
            fireActionsUpdated();
        }

        @Override
        @RequiredUIAccess
        public void hideComponent() {
            if (!myVisible) {
                return;
            }
            myVisible = false;
            myToolbar.updateActionsImmediately();
            fireActionsUpdated();
        }

        @Override
        public void repaintComponent() {
            repaint();
        }

        void fireActionsUpdated() {
            setVisible(myVisible && hasVisibleActions());
        }
    }
}
