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

import consulo.desktop.awt.ui.impl.layout.DesktopTabImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBTabbedPane;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.AbstractPainter;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.IdeGlassPaneUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.impl.BaseSpotlight;
import consulo.ui.layout.Layout;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class DesktopSpotlightImpl extends BaseSpotlight {
    public static final String SECTION_TARGET = "DesktopSpotlightImpl.sectionTarget";

    private static final JBColor BORDER_COLOR = JBColor.namedColor("Settings.Spotlight.borderColor", ColorUtil.toAlpha(JBColor.ORANGE, 100));

    private static final int STROKE = 2;
    private static final int HORIZONTAL_INSET = 8;
    private static final int VERTICAL_INSET = 6;
    private static final int SECTION_VERTICAL_INSET = 2;
    private static final int TAB_HORIZONTAL_INSET = 21;
    private static final int TAB_VERTICAL_INSET = 11;
    private static final int MAX_ARC = 30;
    private static final int DIM_ALPHA = 100;

    private final class SpotlightPainter extends AbstractPainter {
        @Override
        public boolean needsRepaint() {
            return !myLitComponents.isEmpty();
        }

        @Override
        public void executePaint(java.awt.Component component, Graphics2D g) {
            paintSpotlight(g);
        }
    }

    private final @Nullable JComponent myHost;

    private final SpotlightPainter myPainter = new SpotlightPainter();

    private final Disposable myInstallation = Disposable.newDisposable("DesktopSpotlightImpl");

    private final List<java.awt.Component> myLitComponents = new ArrayList<>();

    private final Set<java.awt.Component> mySections = new HashSet<>();

    private final Set<java.awt.Component> myTabs = new HashSet<>();

    private final ComponentListener myBoundsListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            repaintSpotlight();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            repaintSpotlight();
        }

        @Override
        public void componentShown(ComponentEvent e) {
            repaintSpotlight();
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            repaintSpotlight();
        }
    };

    private final HierarchyBoundsListener myAncestorListener = new HierarchyBoundsAdapter() {
        @Override
        public void ancestorMoved(HierarchyEvent e) {
            repaintSpotlight();
        }

        @Override
        public void ancestorResized(HierarchyEvent e) {
            repaintSpotlight();
        }
    };

    @RequiredUIAccess
    public DesktopSpotlightImpl(Layout<?> layout) {
        super(layout);

        JComponent host = TargetAWT.to(layout) instanceof JComponent jComponent ? jComponent : null;
        myHost = host;

        if (host == null || isDisposed()) {
            return;
        }

        host.addComponentListener(myBoundsListener);

        IdeGlassPaneUtil.installPainter(host, myPainter, myInstallation);
    }

    @RequiredUIAccess
    @Override
    protected void onTargetsChanged(List<Component> oldTargets, List<Component> newTargets) {
        if (myHost == null) {
            return;
        }

        unwatchTargets();

        for (Component target : newTargets) {
            java.awt.Component litComponent = TargetAWT.to(target);
            if (litComponent == null || litComponent == myHost) {
                continue;
            }

            watch(litComponent);

            if (target instanceof Layout<?> || isClientFlag(litComponent, SECTION_TARGET)) {
                mySections.add(litComponent);
            }
            else if (isClientFlag(litComponent, JBTabbedPane.LABEL_FROM_TABBED_PANE)) {
                myTabs.add(litComponent);
            }
        }

        for (Tab tab : getTabTargets()) {
            if (tab instanceof DesktopTabImpl desktopTab) {
                java.awt.Component litComponent = desktopTab.getTabComponent();
                watch(litComponent);
                myTabs.add(litComponent);
            }
        }

        repaintSpotlight();
    }

    @Override
    protected void onDispose(List<Component> oldTargets) {
        if (myHost == null) {
            return;
        }

        unwatchTargets();

        myHost.removeComponentListener(myBoundsListener);

        repaintSpotlight();

        Disposer.dispose(myInstallation);
    }

    private static boolean isClientFlag(java.awt.Component component, String key) {
        return component instanceof JComponent jComponent && Boolean.TRUE.equals(jComponent.getClientProperty(key));
    }

    private void watch(java.awt.Component litComponent) {
        litComponent.addComponentListener(myBoundsListener);
        litComponent.addHierarchyBoundsListener(myAncestorListener);

        myLitComponents.add(litComponent);
    }

    private void unwatchTargets() {
        for (java.awt.Component litComponent : myLitComponents) {
            litComponent.removeComponentListener(myBoundsListener);
            litComponent.removeHierarchyBoundsListener(myAncestorListener);
        }

        myLitComponents.clear();
        mySections.clear();
        myTabs.clear();
    }

    private void repaintSpotlight() {
        myPainter.setNeedsRepaint(true);
    }

    private void paintSpotlight(Graphics2D g) {
        JComponent host = myHost;
        if (host == null || myLitComponents.isEmpty()) {
            return;
        }

        Rectangle visibleRect = host.getVisibleRect();
        if (visibleRect.isEmpty()) {
            return;
        }

        Area mask = new Area(new Rectangle(
            visibleRect.x - STROKE,
            visibleRect.y - STROKE,
            visibleRect.width + STROKE * 2,
            visibleRect.height + STROKE * 2
        ));

        for (java.awt.Component litComponent : myLitComponents) {
            Area hole = getHole(host, litComponent, visibleRect, mySections.contains(litComponent), myTabs.contains(litComponent));
            if (hole != null) {
                mask.subtract(hole);
            }
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.clip(visibleRect);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

            Color background = host.getBackground();
            g2.setColor(ColorUtil.toAlpha((background == null ? UIUtil.getPanelBackground() : background).darker(), DIM_ALPHA));
            g2.fill(mask);

            g2.setStroke(new BasicStroke(STROKE));
            g2.setColor(BORDER_COLOR);
            g2.draw(mask);
        }
        finally {
            g2.dispose();
        }
    }

    private static @Nullable Area getHole(
        JComponent host,
        java.awt.Component litComponent,
        Rectangle visibleRect,
        boolean section,
        boolean tab
    ) {
        if (!litComponent.isShowing() || !SwingUtilities.isDescendingFrom(litComponent, host)) {
            return null;
        }

        Point location = SwingUtilities.convertPoint(litComponent, 0, 0, host);

        Insets insets = !section && litComponent instanceof JComponent jComponent ? jComponent.getInsets() : null;
        int left = insets == null ? 0 : insets.left;
        int top = insets == null ? 0 : insets.top;
        int right = insets == null ? 0 : insets.right;
        int bottom = insets == null ? 0 : insets.bottom;

        int horizontalInset = tab ? TAB_HORIZONTAL_INSET : HORIZONTAL_INSET;
        int verticalInset = tab ? TAB_VERTICAL_INSET : section ? SECTION_VERTICAL_INSET : VERTICAL_INSET;

        int x = location.x - horizontalInset + left;
        int y = location.y - verticalInset + top;
        int width = litComponent.getWidth() + horizontalInset * 2 - left - right;
        int height = litComponent.getHeight() + verticalInset * 2 - top - bottom;
        int arc = Math.min(height, MAX_ARC);

        Area hole = new Area(new RoundRectangle2D.Double(x, y, width, height, arc, arc));
        hole.intersect(new Area(getClip(host, litComponent, visibleRect)));
        return hole;
    }

    private static Rectangle getClip(JComponent host, java.awt.Component litComponent, Rectangle visibleRect) {
        Rectangle clip = new Rectangle(visibleRect);
        for (Container parent = litComponent.getParent(); parent != null && parent != host; parent = parent.getParent()) {
            if (parent instanceof JViewport viewport) {
                clip = clip.intersection(SwingUtilities.convertRectangle(viewport, new Rectangle(viewport.getSize()), host));
            }
        }
        return clip;
    }
}
