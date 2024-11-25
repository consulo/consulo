package com.jidesoft.plaf.basic;

import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.swing.ComponentStateSupport;
import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSwingUtilities;
import com.jidesoft.utils.ColorUtils;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;
import java.awt.*;

/**
 * Painter for JIDE styles.
 * <p/>
 * Please note, this class is an internal class which is meant to be used by other JIDE classes only. Future version
 * might break your build if you use it.
 */
public class BasicPainter implements SwingConstants, ThemePainter {
    private static BasicPainter _instance;

    public static ThemePainter getInstance() {
        if (_instance == null) {
            _instance = new BasicPainter();
        }
        return _instance;
    }

    protected Color _bk0;
    protected Color _bk1;
    protected Color _bk2;
    protected Color _bk3;
    protected Color _borderColor;

    public BasicPainter() {
    }

    public void installDefaults() {
        if (_bk0 == null) {
            _bk0 = UIDefaultsLookup.getColor("JideButton.background");
        }
        if (_bk1 == null) {
            _bk1 = UIDefaultsLookup.getColor("JideButton.focusedBackground");
        }
        if (_bk2 == null) {
            _bk2 = UIDefaultsLookup.getColor("JideButton.selectedBackground");
        }
        if (_bk3 == null) {
            _bk3 = UIDefaultsLookup.getColor("JideButton.selectedAndFocusedBackground");
        }
        if (_borderColor == null) {
            _borderColor = UIDefaultsLookup.getColor("JideButton.borderColor");
        }
    }

    public void uninstallDefaults() {
        _borderColor = null;
        _bk0 = null;
        _bk1 = null;
        _bk2 = null;
        _bk3 = null;
    }

    @Override
    public Color getGripperForeground() {
        return UIDefaultsLookup.getColor("Gripper.foreground");
    }

    @Override
    public Color getGripperForegroundLt() {
        return UIDefaultsLookup.getColor("JideButton.highlight");
    }

    @Override
    public Color getSeparatorForeground() {
        return UIDefaultsLookup.getColor("JideButton.shadow");
    }

    @Override
    public Color getSeparatorForegroundLt() {
        return UIDefaultsLookup.getColor("JideButton.highlight");
    }

    @Override
    public Color getCollapsiblePaneContentBackground() {
        return UIDefaultsLookup.getColor("CollapsiblePane.contentBackground");
    }

    @Override
    public Color getCollapsiblePaneTitleForeground() {
        return UIDefaultsLookup.getColor("CollapsiblePane.foreground");
    }

    @Override
    public Color getCollapsiblePaneTitleForegroundEmphasized() {
        return UIDefaultsLookup.getColor("CollapsiblePane.emphasizedForeground");
    }

    @Override
    public Color getCollapsiblePaneFocusTitleForegroundEmphasized() {
        return UIDefaultsLookup.getColor("CollapsiblePane.emphasizedForeground");
    }

    @Override
    public Color getCollapsiblePaneFocusTitleForeground() {
        return UIDefaultsLookup.getColor("CollapsiblePane.foreground");
    }

    @Override
    public Icon getCollapsiblePaneUpIcon() {
        return UIDefaultsLookup.getIcon("CollapsiblePane.upIcon");
    }

    @Override
    public Icon getCollapsiblePaneDownIcon() {
        return UIDefaultsLookup.getIcon("CollapsiblePane.downIcon");
    }

    @Override
    public Icon getCollapsiblePaneUpIconEmphasized() {
        return getCollapsiblePaneUpIcon();
    }

    @Override
    public Icon getCollapsiblePaneDownIconEmphasized() {
        return getCollapsiblePaneDownIcon();
    }

    @Override
    public Icon getCollapsiblePaneTitleButtonBackground() {
        return UIDefaultsLookup.getIcon("CollapsiblePane.titleButtonBackground");
    }

    @Override
    public Icon getCollapsiblePaneTitleButtonBackgroundEmphasized() {
        return UIDefaultsLookup.getIcon("CollapsiblePane.titleButtonBackground.emphasized");
    }

    @Override
    public Icon getCollapsiblePaneUpMask() {
        return getCollapsiblePaneUpIcon();
    }

    @Override
    public Icon getCollapsiblePaneDownMask() {
        return getCollapsiblePaneDownIcon();
    }

    @Override
    public Color getBackgroundDk() {
        return UIDefaultsLookup.getColor("JideButton.background");
    }

    @Override
    public Color getBackgroundLt() {
        return UIDefaultsLookup.getColor("JideButton.background");
    }

    @Override
    public Color getSelectionSelectedDk() {
        return _bk2;
    }

    @Override
    public Color getSelectionSelectedLt() {
        return _bk2;
    }

    @Override
    public Color getMenuItemBorderColor() {
        return UIDefaultsLookup.getColor("MenuItem.selectionBorderColor");
    }

    @Override
    public Color getMenuItemBackground() {
        return UIDefaultsLookup.getColor("MenuItem.background");
    }

    @Override
    public Color getCommandBarTitleBarBackground() {
        return UIDefaultsLookup.getColor("CommandBar.titleBarBackground");
    }

    @Override
    public Color getControl() {
        return UIDefaultsLookup.getColor("JideButton.background");
    }

    @Override
    public Color getControlLt() {
        return getControlShadow();
    }

    @Override
    public Color getControlDk() {
        return getControlShadow();
    }

    @Override
    public Color getControlShadow() {
        return UIDefaultsLookup.getColor("JideButton.shadow");
    }

    @Override
    public Color getTitleBarBackground() {
        return UIDefaultsLookup.getColor("DockableFrame.activeTitleBackground");
    }

    @Override
    public Color getDockableFrameTitleBarActiveForeground() {
        return UIDefaultsLookup.getColor("DockableFrame.activeTitleForeground");
    }

    @Override
    public Color getDockableFrameTitleBarInactiveForeground() {
        return UIDefaultsLookup.getColor("DockableFrame.inactiveTitleForeground");
    }

    @Override
    public Color getTabbedPaneSelectDk() {
        return UIDefaultsLookup.getColor("JideTabbedPane.selectedTabBackgroundDk");
    }

    @Override
    public Color getTabbedPaneSelectLt() {
        return UIDefaultsLookup.getColor("JideTabbedPane.selectedTabBackgroundlt");
    }

    @Override
    public Color getTabAreaBackgroundDk() {
        return UIDefaultsLookup.getColor("JideTabbedPane.tabAreaBackgroundDk");
    }

    @Override
    public Color getTabAreaBackgroundLt() {
        return UIDefaultsLookup.getColor("JideTabbedPane.tabAreaBackgroundLt");
    }

    @Override
    public Color getOptionPaneBannerForeground() {
        return new ColorUIResource(255, 255, 255);
    }

    @Override
    public Color getOptionPaneBannerDk() {
        return new ColorUIResource(45, 96, 249);
    }

    @Override
    public Color getOptionPaneBannerLt() {
        return new ColorUIResource(0, 52, 206);
    }

    @Override
    public void paintSelectedMenu(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        Color oldColor = g.getColor();
        g.setColor(UIDefaultsLookup.getColor("JideButton.darkShadow"));
        g.drawLine(rect.x, rect.y + rect.height, rect.x, rect.y + 1);
        g.drawLine(rect.x + rect.width - 2, rect.y, rect.x + rect.width - 2, rect.y + rect.height);
        if (orientation == SwingConstants.HORIZONTAL) {
            g.drawLine(rect.x, rect.y, rect.x + rect.width - 3, rect.y);
        }
        else {
            g.drawLine(rect.x, rect.y + rect.height - 1, rect.x + rect.width - 3, rect.y + rect.height - 1);
        }
        g.setColor(oldColor);
    }

    @Override
    public void paintMenuItemBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        paintMenuItemBackground(c, g, rect, orientation, state, true);
    }

    @Override
    public void paintMenuItemBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state, boolean showBorder) {
        paintButtonBackground(c, g, rect, orientation, state, showBorder);
    }

    @Override
    public void paintButtonBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        paintButtonBackground(c, g, rect, orientation, state, true);
    }

    @Override
    public void paintButtonBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state, boolean showBorder) {
        installDefaults();
        Color background = null;

        Boolean highContrast = UIManager.getBoolean("Theme.highContrast");
        if (highContrast) {
            background = c.getBackground();
            paintBackground(c, g, rect, state == STATE_DEFAULT || state == STATE_DISABLE ? null : _borderColor,
                state == STATE_PRESSED || state == STATE_SELECTED || state == STATE_ROLLOVER ? UIDefaultsLookup.getColor("JideButton.selectedBackground") : background, orientation);
            return;
        }

        switch (state) {
            case STATE_DEFAULT:
                background = c.getBackground();
                if (background == null || background instanceof UIResource) {
                    background = _bk0;
                }
                paintBackground(c, g, rect, showBorder ? _borderColor : null, background, orientation);
                break;
            case STATE_ROLLOVER:
                if (c instanceof ComponentStateSupport) {
                    background = ((ComponentStateSupport) c).getBackgroundOfState(STATE_ROLLOVER);
                }
                if (background == null || background instanceof UIResource) {
                    background = _bk1;
                }
                paintBackground(c, g, rect, showBorder ? _borderColor : null, background, orientation);
                break;
            case STATE_SELECTED:
                if (c instanceof ComponentStateSupport) {
                    background = ((ComponentStateSupport) c).getBackgroundOfState(STATE_SELECTED);
                }
                if (background == null || background instanceof UIResource) {
                    background = _bk2;
                }
                paintBackground(c, g, rect, showBorder ? _borderColor : null, background, orientation);
                break;
            case STATE_DISABLE_SELECTED:
                if (c instanceof ComponentStateSupport) {
                    background = ((ComponentStateSupport) c).getBackgroundOfState(STATE_SELECTED);
                }
                if (background == null || background instanceof UIResource) {
                    background = _bk2;
                }
                paintBackground(c, g, rect, showBorder ? ColorUtils.toGrayscale(_borderColor) : null, ColorUtils.toGrayscale(background), orientation);
                break;
            case STATE_PRESSED:
                if (c instanceof ComponentStateSupport) {
                    background = ((ComponentStateSupport) c).getBackgroundOfState(STATE_PRESSED);
                }
                if (background == null || background instanceof UIResource) {
                    background = _bk3;
                }
                paintBackground(c, g, rect, showBorder ? _borderColor : null, background, orientation);
                break;
        }
    }

    protected void paintBackground(JComponent c, Graphics g, Rectangle rect, Color borderColor, Color background, int orientation) {
        Color oldColor = g.getColor();
        if (borderColor != null) {
            boolean paintDefaultBorder = true;
            Object o = c.getClientProperty("JideButton.paintDefaultBorder");
            if (o instanceof Boolean) {
                paintDefaultBorder = (Boolean) o;
            }
            if (paintDefaultBorder) {
                g.setColor(borderColor);
                Object position = c.getClientProperty(JideButton.CLIENT_PROPERTY_SEGMENT_POSITION);
                if (position == null || JideButton.SEGMENT_POSITION_ONLY.equals(position)) {
                    g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
                }
                else if (JideButton.SEGMENT_POSITION_FIRST.equals(position)) {
                    if (orientation == SwingConstants.HORIZONTAL) {
                        g.drawRect(rect.x, rect.y, rect.width, rect.height - 1);
                    }
                    else {
                        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height);
                    }
                }
                else if (JideButton.SEGMENT_POSITION_MIDDLE.equals(position)) {
                    if (orientation == SwingConstants.HORIZONTAL) {
                        g.drawRect(rect.x, rect.y, rect.width, rect.height - 1);
                    }
                    else {
                        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height);
                    }
                }
                else if (JideButton.SEGMENT_POSITION_LAST.equals(position)) {
                    if (orientation == SwingConstants.HORIZONTAL) {
                        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
                    }
                    else {
                        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
                    }
                }
            }
            g.setColor(background);
            g.fillRect(rect.x + 1, rect.y + 1, rect.width - 2, rect.height - 2);
        }
        else {
            g.setColor(background);
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
        g.setColor(oldColor);
    }

    @Override
    public void paintChevronBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        if (state != STATE_DEFAULT) {
            paintButtonBackground(c, g, rect, orientation, state);
        }
    }

    @Override
    public void paintDividerBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        Color oldColor = g.getColor();
        g.setColor(UIDefaultsLookup.getColor("SplitPane.background"));
        g.fillRect(0, 0, rect.width, rect.height);
        g.setColor(oldColor);
    }

    @Override
    public void paintCommandBarBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        g.setColor(UIDefaultsLookup.getColor("CommandBar.background"));
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 2, 2);
    }

    @Override
    public void paintFloatingCommandBarBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        g.setColor(UIDefaultsLookup.getColor("CommandBar.background"));
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public void paintMenuShadow(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        Color oldColor = g.getColor();
        g.setColor(UIDefaultsLookup.getColor("MenuItem.shadowColor"));
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
        g.setColor(oldColor);
    }

    @Override
    public void paintContentBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        g.setColor(UIDefaultsLookup.getColor("control"));
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public void paintStatusBarBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        if (c.isOpaque()) {
            paintContentBackground(c, g, rect, orientation, state);
        }
    }

    @Override
    public void paintCommandBarTitlePane(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        g.setColor(getCommandBarTitleBarBackground());
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public void paintGripper(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
//        int w = Math.min(30, rect.width);
//        int h = rect.height;

        // basic painter always use horizontal line to paint grippers. It's just they are short and more lines when paints vertical gripper
        // and long and fewer lines when paints horizontally.
        g.setColor(getGripperForeground());

        // shrink the rect size
        if (rect.width > rect.height) {
            rect.x = rect.x + rect.width / 2 - 10;
            rect.width = 22;
        }
        else {
            rect.y = rect.y + rect.height / 2 - 10;
            rect.height = 22;
        }

        if (orientation == SwingConstants.HORIZONTAL) {
            if (rect.width <= 30) {
                final int MARGIN = 3;
                for (int i = 0; i < (rect.height - 2 * MARGIN) / 2; i++) {
                    g.drawLine(rect.x + 3, rect.y + MARGIN + i * 2, rect.x + rect.width - MARGIN, rect.y + MARGIN + i * 2);
                }
            }
            else { // for gripper in popup
                final int MARGIN = 2;
                for (int i = 0; i < (rect.height - 2 * MARGIN) / 2; i++) {
                    g.drawLine((rect.width - rect.width) / 2, rect.y + MARGIN + i * 2, (rect.width + rect.width) / 2, rect.y + MARGIN + i * 2);
                }
            }
        }
        else {
            final int MARGIN = 3;
            int count = (rect.width - 2 * MARGIN) / 2;
            for (int i = 0; i < count; i++) {
                int x = rect.x + rect.width / 2 - count + i * 2;
                g.drawLine(x, rect.y + MARGIN, x, rect.y + rect.height - MARGIN);
            }
        }
    }

    @Override
    public void paintChevronMore(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        g.setColor(UIDefaultsLookup.getColor("CommandBar.darkShadow"));
        if (orientation == SwingConstants.HORIZONTAL) {
            if (!c.getComponentOrientation().isLeftToRight()) {
                int y = rect.y + 4;
                for (int i = -2; i <= 2; i++) {
                    int offset = Math.abs(i);
                    g.drawLine(rect.x + 2 + offset, y, rect.x + 3 + offset, y);
                    g.drawLine(rect.x + 6 + offset, y, rect.x + 7 + offset, y);
                    y++;
                }
            }
            else {
                int y = rect.y + 4;
                for (int i = -2; i <= 2; i++) {
                    int offset = -Math.abs(i);
                    g.drawLine(rect.x + 4 + offset, y, rect.x + 5 + offset, y);
                    g.drawLine(rect.x + 8 + offset, y, rect.x + 9 + offset, y);
                    y++;
                }
            }
        }
        else if (orientation == SwingConstants.VERTICAL) {
            int x = rect.x + 4;
            for (int i = -2; i <= 2; i++) {
                int offset = -Math.abs(i);
                g.drawLine(x, rect.y + 4 + offset, x, rect.y + 5 + offset);
                g.drawLine(x, rect.y + 8 + offset, x, rect.y + 9 + offset);
                x++;
            }
        }
    }

    @Override
    public void paintChevronOption(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        int startX;
        int startY;
        if (orientation == SwingConstants.HORIZONTAL || !c.getComponentOrientation().isLeftToRight()) {
            startX = rect.x + 3;
            startY = rect.y + rect.height - 7;
        }
        else {
            startX = rect.x + rect.width - 7;
            startY = rect.y + 3;
        }
        if (orientation == SwingConstants.HORIZONTAL || !c.getComponentOrientation().isLeftToRight()) {
            JideSwingUtilities.paintArrow(g, UIDefaultsLookup.getColor("CommandBar.darkShadow"), startX, startY, 5, SwingConstants.HORIZONTAL);
        }
        else {
            JideSwingUtilities.paintArrow(g, UIDefaultsLookup.getColor("CommandBar.darkShadow"), startX, startY, 5, orientation);
        }
    }

    @Override
    public void paintFloatingChevronOption(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        int startX = rect.width / 2 - 4;
        int startY = rect.height / 2 - 2;
        if (state == STATE_ROLLOVER) {
            JideSwingUtilities.paintArrow(g, Color.BLACK, startX, startY, 9, orientation);
        }
        else {
            JideSwingUtilities.paintArrow(g, Color.WHITE, startX, startY, 9, orientation);
        }
    }

    @Override
    public void paintDockableFrameBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        if (!c.isOpaque()) {
            return;
        }
        g.setColor(UIDefaultsLookup.getColor("DockableFrame.background"));
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public void paintDockableFrameTitlePane(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        int x = rect.x;
        int y = rect.y;
        int w = rect.width - 1;
        int h = rect.height;
        if (c.getBorder() != null) {
            Insets insets = c.getBorder().getBorderInsets(c);
            x += insets.left;
            y += insets.top;
            w -= insets.right + insets.left;
            h -= insets.top + insets.bottom;
        }
        rect = new Rectangle(x + 1, y + 1, w - 1, h - 1);

        Boolean highContrast = UIManager.getBoolean("Theme.highContrast");
        if (state == STATE_SELECTED) {
            g.setColor(UIDefaultsLookup.getColor("DockableFrame.activeTitleBorderColor"));
            g.drawRect(x, y, w, h);
            g.setColor(highContrast ? UIDefaultsLookup.getColor("JideButton.selectedBackground") : UIDefaultsLookup.getColor("DockableFrame.activeTitleBackground"));
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
        else {
            g.setColor(UIDefaultsLookup.getColor("DockableFrame.inactiveTitleBorderColor"));
            g.drawRoundRect(x, y, w, h, 2, 2);
            g.setColor(UIDefaultsLookup.getColor("DockableFrame.inactiveTitleBackground"));
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
    }

    @Override
    public void paintCollapsiblePaneTitlePaneBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        Boolean highContrast = UIManager.getBoolean("Theme.highContrast");
        if (!(c.getBackground() instanceof UIResource)) {
            g.setColor(c.getBackground());
        }
        else {
            g.setColor(UIDefaultsLookup.getColor(highContrast ? "JideButton.background" : "CollapsiblePane.background"));
        }
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
        if (highContrast) {
            g.setColor(UIDefaultsLookup.getColor("CollapsiblePane.background"));
            g.drawRect(rect.x, rect.y, rect.width - 1, rect.height);
        }
    }

    @Override
    public void paintCollapsiblePaneTitlePaneBackgroundEmphasized(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        if (!(c.getBackground() instanceof UIResource)) {
            g.setColor(c.getBackground());
        }
        else {
            g.setColor(UIDefaultsLookup.getColor("CollapsiblePane.emphasizedBackground"));
        }
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public void paintCollapsiblePanesBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        if (!c.isOpaque()) {
            return;
        }

        if (!(c.getBackground() instanceof UIResource)) {
            g.setColor(c.getBackground());
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
        else {
            g.setColor(UIDefaultsLookup.getColor("TextField.background"));
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
    }

    @Override
    public void paintCollapsiblePaneTitlePaneBackgroundPlainEmphasized(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        g.setColor(UIDefaultsLookup.getColor("CollapsiblePane.emphasizedBackground"));
        g.drawLine(rect.x, rect.y + rect.height - 1, rect.x + rect.width, rect.y + rect.height - 1);
    }

    @Override
    public void paintCollapsiblePaneTitlePaneBackgroundPlain(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        if (!(c.getBackground() instanceof UIResource)) {
            g.setColor(c.getBackground());
        }
        else {
            g.setColor(UIDefaultsLookup.getColor("CollapsiblePane.background"));
        }
        switch (orientation) {
            case SwingConstants.EAST:
                g.drawLine(rect.x + rect.width - 1, rect.y, rect.x + rect.width - 1, rect.y + rect.height - 1);
                break;
            case SwingConstants.WEST:
                g.drawLine(rect.x, rect.y, rect.x, rect.y + rect.height - 1);
                break;
            case SwingConstants.NORTH:
                g.drawLine(rect.x, rect.y, rect.x + rect.width, rect.y);
            case SwingConstants.SOUTH:
            default:
                g.drawLine(rect.x, rect.y + rect.height - 1, rect.x + rect.width, rect.y + rect.height - 1);
                break;
        }
    }

    @Override
    public void paintCollapsiblePaneTitlePaneBackgroundSeparatorEmphasized(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        g.setColor(UIManager.getColor("CollapsiblePane.emphasizedBackground"));
        g.fillRect(rect.x, rect.y, rect.x + rect.width, rect.height);
    }

    @Override
    public void paintCollapsiblePaneTitlePaneBackgroundSeparator(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        g.setColor(UIManager.getColor("CollapsiblePane.background"));
        g.fillRect(rect.x, rect.y, rect.x + rect.width, rect.height);
    }

    @Override
    public Color getColor(Object key) {
        return UIDefaultsLookup.getColor(key);
    }

    @Override
    public void paintTabAreaBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {

    }

    @Override
    public void paintTabBackground(JComponent c, Graphics g, Shape region, Color[] colors, int orientation, int state) {

    }

    @Override
    public void paintTabContentBorder(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {

    }

    @Override
    public void paintSidePaneItemBackground(JComponent c, Graphics g, Rectangle rect, Color[] colors, int orientation, int state) {

    }

    @Override
    public void paintHeaderBoxBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
    }

    @Override
    public void paintToolBarSeparator(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
        if (c.isOpaque()) {
            g.setColor(c.getBackground());
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
        if (orientation == SwingConstants.HORIZONTAL) {
            g.setColor(c.getForeground());
            g.drawLine(rect.x + rect.width / 2, rect.y + 1, rect.x + rect.width / 2, rect.y + rect.height - 2);
        }
        else { // HORIZONTAL
            g.setColor(c.getForeground());
            g.drawLine(rect.x + 1, rect.y + rect.height / 2, rect.x + rect.width - 2, rect.y + rect.height / 2);
        }
    }

    @Override
    public void paintPopupMenuSeparator(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {

    }

    @Override
    public void paintStatusBarSeparator(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {

    }

    /**
     * The distant from top edge of the table header to the top edge of the sort arrow.
     */
    public static int V_GAP = 2;

    /**
     * The distant from the right edge of the table header to left edge of sort arrow.
     */
    public static int H_GAP = 5;

    /**
     * The gap between the sort arrow and index text.
     */
    public static int ARROW_TEXT_GAP = 0;

    /**
     * Should the arrow be displayed on the top of the header.
     *
     * @return true to display the sort arrow on top. Otherwise false.
     */
    protected boolean shouldDisplayOnTop() {
        return false;
    }

    @Override
    public void fillBackground(JComponent c, Graphics g, Rectangle rect, int orientation, int state, Color color) {
        Color oldColor = g.getColor();
        g.setColor(color);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
        g.setColor(oldColor);
    }

    @Override
    public Insets getSortableTableHeaderColumnCellDecoratorInsets(JComponent c, Graphics g, Rectangle rect, int orientation, int state, int sortOrder, Icon sortIcon, int orderIndex, Color indexColor, boolean paintIndex) {
        if (shouldDisplayOnTop()) {
            return null;
        }
        int iconWidth = sortIcon == null ? 0 : sortIcon.getIconWidth();
        int textWidthAndGap = 0;
        if (paintIndex && orderIndex != -1) {
            Font font;
            if (g != null) {
                Font oldFont = g.getFont();
                font = g.getFont().deriveFont(Font.PLAIN, oldFont.getSize() - 3);
            }
            else if (c.getFont() != null) {
                font = c.getFont().deriveFont(Font.PLAIN, c.getFont().getSize() - 3);
            }
            else {
                font = Font.getFont("Arial");
            }
            String str = "" + (orderIndex + 1);
            int textWidth = SwingUtilities.computeStringWidth(c.getFontMetrics(font), str);
            textWidthAndGap = ARROW_TEXT_GAP + textWidth + H_GAP;
        }
        else {
            textWidthAndGap = H_GAP;
        }
        if (textWidthAndGap + iconWidth == 0) {
            return null;
        }
        return new Insets(0, 0, 0, textWidthAndGap + iconWidth);
    }

    @Override
    public void paintSortableTableHeaderColumn(JComponent c, Graphics g, Rectangle rect, int orientation, int state, int sortOrder, Icon sortIcon, int orderIndex, Color indexColor, boolean paintIndex) {

    }
}