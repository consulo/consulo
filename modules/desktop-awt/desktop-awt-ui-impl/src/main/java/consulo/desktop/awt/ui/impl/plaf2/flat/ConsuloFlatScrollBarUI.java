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
package consulo.desktop.awt.ui.impl.plaf2.flat;

import com.formdev.flatlaf.ui.FlatScrollBarUI;
import consulo.desktop.awt.ui.impl.plaf.IslandPainter;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.internal.laf.DefaultScrollBarAnimationBehavior;
import consulo.ui.ex.awt.internal.laf.MacScrollBarAnimationBehavior;
import consulo.ui.ex.awt.internal.laf.ScrollBarAnimationBehavior;
import consulo.ui.ex.awt.internal.laf.TwoWayAnimator;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Draws a scroll bar the way FlatLaf does, while the visibility of its track and thumb follows the scroll bar
 * animation: the track fades in on hover, and over the content of a scroll pane the thumb only shows while
 * scrolling.
 */
public class ConsuloFlatScrollBarUI extends FlatScrollBarUI {
    public static ComponentUI createUI(JComponent c) {
        return new ConsuloFlatScrollBarUI();
    }

    private @Nullable ScrollBarAnimationBehavior myAnimationBehavior;

    private final MouseAdapter myHoverListener = new MouseAdapter() {
        private boolean myOverTrack;
        private boolean myOverThumb;

        @Override
        public void mouseMoved(MouseEvent e) {
            update(e, true);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            update(e, true);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            update(e, true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            update(e, false);
        }

        private void update(MouseEvent e, boolean inside) {
            ScrollBarAnimationBehavior behavior = myAnimationBehavior;
            if (behavior == null) {
                return;
            }

            boolean overThumb = inside && getThumbBounds().contains(e.getPoint());
            if (myOverTrack != inside) {
                behavior.onTrackHover(myOverTrack = inside);
            }
            if (myOverThumb != overThumb) {
                behavior.onThumbHover(myOverThumb = overThumb);
            }
        }
    };

    private final AdjustmentListener myAdjustmentListener = e -> {
        ScrollBarAnimationBehavior behavior = myAnimationBehavior;
        if (behavior != null) {
            behavior.onThumbMove();
        }
    };

    @Override
    protected void installDefaults() {
        super.installDefaults();

        LookAndFeel.installProperty(scrollbar, "opaque", false);
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        myAnimationBehavior = createAnimationBehavior((JScrollBar) c);
    }

    @Override
    public void uninstallUI(JComponent c) {
        ScrollBarAnimationBehavior behavior = myAnimationBehavior;
        if (behavior != null) {
            behavior.onUninstall();
        }
        myAnimationBehavior = null;

        super.uninstallUI(c);
    }

    @Override
    protected void installListeners() {
        super.installListeners();

        scrollbar.addMouseListener(myHoverListener);
        scrollbar.addMouseMotionListener(myHoverListener);
        scrollbar.addAdjustmentListener(myAdjustmentListener);
    }

    @Override
    protected void uninstallListeners() {
        scrollbar.removeMouseListener(myHoverListener);
        scrollbar.removeMouseMotionListener(myHoverListener);
        scrollbar.removeAdjustmentListener(myAdjustmentListener);

        super.uninstallListeners();
    }

    private ScrollBarAnimationBehavior createAnimationBehavior(JScrollBar scrollBar) {
        TwoWayAnimator trackAnimator = createAnimator("ScrollBarTrack", scrollBar);
        TwoWayAnimator thumbAnimator = createAnimator("ScrollBarThumb", scrollBar);
        if (IslandPainter.isEnabled()) {
            return new MacScrollBarAnimationBehavior(() -> scrollbar, ConsuloFlatScrollBarUI::isOpaque, trackAnimator, thumbAnimator);
        }
        return new DefaultScrollBarAnimationBehavior(trackAnimator, thumbAnimator);
    }

    private static TwoWayAnimator createAnimator(String name, JScrollBar scrollBar) {
        return new TwoWayAnimator(name, 11, 150, 125, 300, 125) {
            @Override
            protected void onValueUpdate() {
                scrollBar.repaint();
            }
        };
    }

    /**
     * A scroll bar takes room beside the content unless a {@link JBScrollPane} lays it over the content.
     */
    private static boolean isOpaque(JScrollBar scrollBar) {
        if (scrollBar.isOpaque()) {
            return true;
        }
        return !(scrollBar.getParent() instanceof JScrollPane pane && pane.getLayout() instanceof JBScrollPane.Layout);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (isOpaque(scrollbar)) {
            Color background = getViewBackground(c);
            if (background != null) {
                g.setColor(background);
                g.fillRect(0, 0, c.getWidth(), c.getHeight());
            }
        }

        super.paint(g, c);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        ScrollBarAnimationBehavior behavior = myAnimationBehavior;
        float frame = behavior == null ? 0 : behavior.getTrackFrame();
        if (frame <= 0 || hoverTrackColor == null) {
            return;
        }
        // over the content the track only shows together with the thumb
        if (behavior instanceof MacScrollBarAnimationBehavior && !isOpaque(scrollbar) && behavior.getThumbFrame() <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, frame)));
            g2.setColor(hoverTrackColor);
            paintTrackOrThumb(g2, c, trackBounds, trackInsets, trackArc);
        }
        finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        ScrollBarAnimationBehavior behavior = myAnimationBehavior;
        if (!(behavior instanceof MacScrollBarAnimationBehavior) || isOpaque(scrollbar)) {
            super.paintThumb(g, c, thumbBounds);
            return;
        }

        float frame = behavior.getThumbFrame();
        if (frame <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, frame)));
            super.paintThumb(g2, c, thumbBounds);
        }
        finally {
            g2.dispose();
        }
    }

    private static @Nullable Color getViewBackground(JComponent c) {
        if (!(c.getParent() instanceof JScrollPane scrollPane)) {
            return null;
        }

        Component view = scrollPane.getViewport().getView();
        return view != null && view.isOpaque() ? view.getBackground() : null;
    }
}
