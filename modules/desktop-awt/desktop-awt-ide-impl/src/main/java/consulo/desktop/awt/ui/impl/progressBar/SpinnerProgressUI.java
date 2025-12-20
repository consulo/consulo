package consulo.desktop.awt.ui.impl.progressBar;

import com.formdev.flatlaf.ui.FlatProgressBarUI;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.Graphics2DProxy;
import com.formdev.flatlaf.util.UIScale;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class SpinnerProgressUI extends FlatProgressBarUI {

    protected SpinnerRender render;
    private PropertyChangeListener propertyChangeListener;
    private Animator animator;
    private float lastAnimator;
    private float animateFrame;
    private boolean moreAnimation;

    private final Rectangle iconRect = new Rectangle();
    private final Rectangle textRect = new Rectangle();
    private final Rectangle viewRect = new Rectangle();

    @Override
    protected void installDefaults() {
        super.installDefaults();
        progressBar.setOpaque(true);
        render = new RingSpinner(3);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        propertyChangeListener = (PropertyChangeEvent evt) -> {
            if (render.isPaintComplete()) {
                String name = evt.getPropertyName();
                switch (name) {
                    case "indeterminate":
                        checkIndeterminate(evt);
                        break;
                }
            }
        };
        progressBar.addPropertyChangeListener(propertyChangeListener);
    }

    private void checkIndeterminate(PropertyChangeEvent evt) {
        boolean oldValue = (boolean) evt.getOldValue();
        boolean newValue = (boolean) evt.getNewValue();
        if (oldValue && !newValue) {
            if (animator == null) {
                animator = new Animator(350, new Animator.TimingTarget() {
                    @Override
                    public void begin() {
                        moreAnimation = true;
                    }

                    @Override
                    public void end() {
                        moreAnimation = false;
                    }

                    @Override
                    public void timingEvent(float f) {
                        animateFrame = f;
                        progressBar.repaint();
                    }
                });
            }
            else {
                if (animator.isRunning()) {
                    animator.cancel();
                }
            }
            moreAnimation = true;
            animateFrame = 0;
            animator.start();
        }
    }

    @Override
    protected void uninstallDefaults() {
        super.uninstallDefaults();
        render = null;
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        animator = null;
    }

    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        progressBar.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {

        // This code take from BasicLabelUI

        SpinnerProgress spinner = (SpinnerProgress) c;
        String text = spinner.isStringPainted() ? spinner.getString() : null;
        Icon icon = spinner.getIcon();
        Insets insets = spinner.getInsets(null);
        Font font = spinner.getFont();

        int space = UIScale.scale(spinner.getSpace()) * 2;
        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;

        if ((icon == null) && ((text == null) || ((text != null) && (font == null)))) {
            int add = UIScale.scale(18);
            return new Dimension(dx + add, dy + add);
        }
        else if ((text == null) || ((icon != null) && (font == null))) {
            int add = UIScale.scale(spinner.getSpace()) * 2;
            return new Dimension(icon.getIconWidth() + dx + add, icon.getIconHeight() + dy + add);
        }
        else {
            FontMetrics fm = spinner.getFontMetrics(font);
            Rectangle iconR = new Rectangle();
            Rectangle textR = new Rectangle();
            Rectangle viewR = new Rectangle();

            iconR.x = iconR.y = iconR.width = iconR.height = 0;
            textR.x = textR.y = textR.width = textR.height = 0;
            viewR.x = dx;
            viewR.y = dy;
            viewR.width = viewR.height = Short.MAX_VALUE;

            layoutCL(spinner, fm, text, icon, viewR, iconR, textR);
            int x1 = Math.min(iconR.x, textR.x);
            int x2 = Math.max(iconR.x + iconR.width, textR.x + textR.width);
            int y1 = Math.min(iconR.y, textR.y);
            int y2 = Math.max(iconR.y + iconR.height, textR.y + textR.height);
            int size = Math.max(x2 - x1, y2 - y1);
            Dimension rv = new Dimension(size, size);

            viewRect.x = insets.left;
            viewRect.y = insets.top;
            viewRect.width = viewRect.height = size;
            rv.width += dx + space;
            rv.height += dy + space;

            return rv;
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (progressBar instanceof SpinnerProgress) {
            SpinnerProgress spinner = (SpinnerProgress) progressBar;
            layout(spinner, g.getFontMetrics(), c.getWidth(), c.getHeight());
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (progressBar.isIndeterminate()) {
                float f = getAnimation();
                render.paintIndeterminate(g2, c, getBox(viewRect), f);
            }
            else {
                if (moreAnimation) {
                    render.paintCompleteIndeterminate(g2, c, getBox(viewRect), lastAnimator, animateFrame, (float) progressBar.getPercentComplete());
                }
                else {
                    render.paintDeterminate(g2, c, getBox(viewRect), (float) progressBar.getPercentComplete());
                }
            }
            if (spinner.getIcon() != null) {
                paintIcon(g, c, iconRect);
            }
            if (spinner.isStringPainted()) {
                paintString(g);
            }
        }
    }

    protected String layoutCL(
        SpinnerProgress spinner,
        FontMetrics fontMetrics,
        String text,
        Icon icon,
        Rectangle viewR,
        Rectangle iconR,
        Rectangle textR) {

        // This code take from BasicLabelUI

        return SwingUtilities.layoutCompoundLabel(
            spinner,
            fontMetrics,
            text,
            icon,
            spinner.getVerticalAlignment(),
            spinner.getHorizontalAlignment(),
            spinner.getVerticalTextPosition(),
            spinner.getHorizontalTextPosition(),
            viewR,
            iconR,
            textR,
            UIScale.scale(spinner.getIconTextGap()));
    }

    private String layout(SpinnerProgress spinner, FontMetrics fm, int width, int height) {
        Insets insets = spinner.getInsets(null);
        String text = spinner.isStringPainted() ? progressBar.getString() : null;
        Icon icon = spinner.getIcon();
        Rectangle paintViewR = new Rectangle();
        paintViewR.x = insets.left;
        paintViewR.y = insets.top;
        paintViewR.width = width - (insets.left + insets.right);
        paintViewR.height = height - (insets.top + insets.bottom);
        iconRect.x = iconRect.y = iconRect.width = iconRect.height = 0;
        textRect.x = textRect.y = textRect.width = textRect.height = 0;
        return layoutCL(spinner, fm, text, icon, paintViewR, iconRect,
            textRect);
    }

    protected void paintString(Graphics g) {
        Graphics2DProxy g2 = new Graphics2DProxy((Graphics2D) g) {
            @Override
            public void setColor(Color c) {
                super.setColor(progressBar.getForeground());
            }
        };
        g2.setColor(getSelectionBackground());
        paintString(g2, textRect.x, textRect.y, textRect.width, textRect.height, 0, null);
    }

    protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect) {
        SpinnerProgress spinner = (SpinnerProgress) progressBar;
        spinner.getIcon().paintIcon(c, g, iconRect.x, iconRect.y);
    }

    private float getAnimation() {
        int index = super.getAnimationIndex();
        float animate = (index / (float) getFrameCount()) * 2f;
        lastAnimator = animate;
        return animate;
    }

    @Override
    protected Rectangle getBox(Rectangle r) {
        if (r == null) {
            return null;
        }
        Insets insets = progressBar.getInsets();
        int width = progressBar.getWidth() - (insets.right + insets.left);
        int height = progressBar.getHeight() - (insets.top + insets.bottom);
        int size = Math.min(width, height);
        int x = insets.left + (width - size) / 2;
        int y = insets.top + (height - size) / 2;
        r.setBounds(x, y, size, size);
        return r;
    }
}