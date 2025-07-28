// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.logging.Logger;
import consulo.ui.ex.awt.scroll.LatchingScroll;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.reflect.ReflectionUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseEvent;

public class JBScrollPane extends JScrollPane {
    /**
     * Supposed to be used as a client property key for scrollbar and indicates if this scrollbar should be ignored
     * when insets for {@code JScrollPane's} content are being calculated.
     * <p>
     * Without this key scrollbar's width is included to content insets when content is {@code JList}. As a result list items cannot intersect with
     * scrollbar.
     * <p>
     * Please use as a marker for scrollbars, that should be transparent and shown over content.
     *
     * @see UIUtil#putClientProperty(JComponent, Key, Object)
     */
    @Deprecated
    public static final Key<Boolean> IGNORE_SCROLLBAR_IN_INSETS = Key.create("IGNORE_SCROLLBAR_IN_INSETS");

    /**
     * When set to {@link Boolean#TRUE} for component then latching will be ignored.
     *
     * @see LatchingScroll
     * @see UIUtil#putClientProperty(JComponent, Key, Object)
     */
    @Deprecated
    public static final Key<Boolean> IGNORE_SCROLL_LATCHING = Key.create("IGNORE_SCROLL_LATCHING");

    private static final Logger LOG = Logger.getInstance(JBScrollPane.class);

    @Deprecated
    public JBScrollPane(int viewportWidth) {
        init(false);
    }

    public JBScrollPane() {
        init();
    }

    public JBScrollPane(Component view) {
        super(view);
        init();
    }

    public JBScrollPane(int vsbPolicy, int hsbPolicy) {
        super(vsbPolicy, hsbPolicy);
        init();
    }

    public JBScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
        super(view, vsbPolicy, hsbPolicy);
        init();
    }

    private void init() {
        init(true);
    }

    private void init(boolean setupCorners) {
        if (setupCorners) {
            setupCorners();
        }
    }

    protected void setupCorners() {
        setBorder(IdeBorderFactory.createBorder());
    }

    public static boolean canBePreprocessed(@Nonnull MouseEvent e, @Nonnull JScrollBar bar) {
        if (e.getID() == MouseEvent.MOUSE_MOVED || e.getID() == MouseEvent.MOUSE_PRESSED) {
            ScrollBarUI ui = bar.getUI();
            if (ui instanceof BasicScrollBarUI) {
                BasicScrollBarUI bui = (BasicScrollBarUI) ui;
                try {
                    Rectangle rect = (Rectangle) ReflectionUtil.getDeclaredMethod(BasicScrollBarUI.class, "getThumbBounds", ArrayUtil.EMPTY_CLASS_ARRAY).invoke(bui);
                    Point point = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), bar);
                    return !rect.contains(point);
                }
                catch (Exception e1) {
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * These client properties show a component position on a scroll pane.
     * It is set by internal layout manager of the scroll pane.
     */
    @Deprecated
    public enum Alignment {
        TOP,
        LEFT,
        RIGHT,
        BOTTOM;

        public static Alignment get(JComponent component) {
            if (component != null) {
                Object property = component.getClientProperty(Alignment.class);
                if (property instanceof Alignment) {
                    return (Alignment) property;
                }

                Container parent = component.getParent();
                if (parent instanceof JScrollPane) {
                    JScrollPane pane = (JScrollPane) parent;
                    if (component == pane.getColumnHeader()) {
                        return TOP;
                    }
                    if (component == pane.getHorizontalScrollBar()) {
                        return BOTTOM;
                    }
                    boolean ltr = pane.getComponentOrientation().isLeftToRight();
                    if (component == pane.getVerticalScrollBar()) {
                        return ltr ? RIGHT : LEFT;
                    }
                    if (component == pane.getRowHeader()) {
                        return ltr ? LEFT : RIGHT;
                    }
                }
                // assume alignment for a scroll bar,
                // which is not contained in a scroll pane
                if (component instanceof JScrollBar) {
                    JScrollBar bar = (JScrollBar) component;
                    switch (bar.getOrientation()) {
                        case Adjustable.HORIZONTAL:
                            return BOTTOM;
                        case Adjustable.VERTICAL:
                            return bar.getComponentOrientation().isLeftToRight() ? RIGHT : LEFT;
                    }
                }
            }
            return null;
        }
    }
}