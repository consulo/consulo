/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.awt.ui.impl.adtui.common;

import consulo.desktop.awt.ui.impl.adtui.TabularLayout;
import consulo.desktop.awt.ui.impl.adtui.stdui.TooltipLayeredPane;
import consulo.platform.Platform;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Contains an assortment of utility functions for the UI tools in this module.
 */
public final class AdtUiUtils {
    private static final String ELLIPSIS = "...";

    /**
     * Default font to be used in the profiler UI.
     */
    public static final Font DEFAULT_FONT = JBUI.Fonts.label(10f);

    /**
     * Default font to be used in an empty tool window. eg Device File Explorer when no device is connected and Sqlite Explorer when no
     * database has been opened.
     */
    public static final Font EMPTY_TOOL_WINDOW_FONT = JBUI.Fonts.label(13f);

    /**
     * Default font color of charts, and component labels.
     */
    public static final Color DEFAULT_FONT_COLOR = JBColor.foreground();

    /**
     * Color to be used by labels representing the title of a Component, e.g. a layout preview or a status button.
     */
    public static final Color TITLE_COLOR = new JBColor(0x6C707E, 0xCED0D6);

    /**
     * Color to be used by labels representing the title in Preview.
     */
    public static final Color HEADER_COLOR = new JBColor(0x6c707e, 0xdfe1e5);

    /**
     * Color to be used by labels representing the title in Preview in hovered state.
     */
    public static final Color HEADER_HOVER_COLOR = new JBColor(0x5a5d6b, 0xf0f1f2);

    public static final Color DEFAULT_BORDER_COLOR = StudioColors.border;

    public static final Border DEFAULT_TOP_BORDER = BorderFactory.createMatteBorder(1, 0, 0, 0, DEFAULT_BORDER_COLOR);

    public static final Border DEFAULT_LEFT_BORDER = BorderFactory.createMatteBorder(0, 1, 0, 0, DEFAULT_BORDER_COLOR);

    public static final Border DEFAULT_BOTTOM_BORDER = BorderFactory.createMatteBorder(0, 0, 1, 0, DEFAULT_BORDER_COLOR);

    public static final Border DEFAULT_RIGHT_BORDER = BorderFactory.createMatteBorder(0, 0, 0, 1, DEFAULT_BORDER_COLOR);

    public static final Border DEFAULT_HORIZONTAL_BORDERS = BorderFactory.createMatteBorder(1, 0, 1, 0, DEFAULT_BORDER_COLOR);

    public static final Border DEFAULT_VERTICAL_BORDERS = BorderFactory.createMatteBorder(0, 1, 0, 1, DEFAULT_BORDER_COLOR);

    public static final GridBagConstraints GBC_FULL =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.BASELINE, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

    public enum ShrinkDirection {
        TRUNCATE_START,
        TRUNCATE_END,
    }

    private AdtUiUtils() {
    }

    /**
     * Collapse a line of text to fit the availableSpace by truncating the string and pad the end with ellipsis.
     *
     * @param text           the original text.
     * @param metrics        the {@link FontMetrics} used to measure the text's width.
     * @param availableSpace the available space to render the text.
     * @param spaceThreshold if availableSpace is not larger than this threshold, return an empty string.
     * @return the fitted text
     */
    public static String shrinkToFit(
        String text,
        FontMetrics metrics,
        float availableSpace,
        float spaceThreshold,
        ShrinkDirection direction
    ) {
        // FontMetrics#stringWidth(String) has some runtime overhead so the threshold is a performance
        // optimization.
        return shrinkToFit(text, direction, s -> availableSpace > spaceThreshold && availableSpace >= metrics.stringWidth(s));
    }

    public static String shrinkToFit(String text, FontMetrics metrics, float availableSpace, float spaceThreshold) {
        return shrinkToFit(text, metrics, availableSpace, spaceThreshold, ShrinkDirection.TRUNCATE_END);
    }

    /**
     * Similar to {@link #shrinkToFit(String, FontMetrics, float, float)}, but instead of a predicate to fit space it uses the font metrics
     * compared to available space.
     */
    public static String shrinkToFit(String text, FontMetrics metrics, float availableSpace, ShrinkDirection direction) {
        return shrinkToFit(text, metrics, availableSpace, 0.0f, direction);
    }

    public static String shrinkToFit(String text, FontMetrics metrics, float availableSpace) {
        return shrinkToFit(text, metrics, availableSpace, ShrinkDirection.TRUNCATE_END);
    }

    public static String shrinkToFit(String text, Predicate<String> textFitPredicate) {
        return shrinkToFit(text, ShrinkDirection.TRUNCATE_END, textFitPredicate);
    }

    /**
     * Collapses a line of text to fit the availableSpace by truncating the string and pad the end with ellipsis.
     *
     * @param text             the original text.
     * @param textFitPredicate predicate to test if text fits.
     * @return the fitted text.
     */
    public static String shrinkToFit(String text, ShrinkDirection direction, Predicate<String> textFitPredicate) {
        if (textFitPredicate.test(text)) {
            // Enough space - early return.
            return text;
        }
        else if (!textFitPredicate.test(ELLIPSIS)) {
            // No space to fit "..." - early return.
            return "";
        }
        int smallestLength = 0;
        int largestLength = text.length();
        int bestLength = smallestLength;
        do {
            int midLength = smallestLength + (largestLength - smallestLength) / 2;
            String substring = direction == ShrinkDirection.TRUNCATE_END
                ? text.substring(0, midLength)
                : text.substring(text.length() - 1 - midLength);
            if (textFitPredicate.test(substring + ELLIPSIS)) {
                bestLength = midLength;
                smallestLength = midLength + 1;
            }
            else {
                largestLength = midLength - 1;
            }
        }
        while (smallestLength <= largestLength);

        // Note: Don't return "..." if that's all we could show
        String result = direction == ShrinkDirection.TRUNCATE_END
            ? text.substring(0, bestLength) + ELLIPSIS
            : ELLIPSIS + text.substring(text.length() - 1 - bestLength);

        return bestLength > 0 ? result : "";
    }

    /**
     * Does the reverse of {@link JBUIScale#scale(int)}
     */
    public static int unscale(int i) {
        return Math.round(i / JBUIScale.scale(1.0f));
    }

    /**
     * Returns the resulting sRGB color (no alpha) by overlaying a foreground color with a given opacity over a background color.
     *
     * @param backgroundRgb     the sRGB color of the background.
     * @param foregroundRbg     the sRGB color of the foreground.
     * @param foregroundOpacity the opacity of the foreground, in the range of 0.0 - 1.0
     */
    public static Color overlayColor(int backgroundRgb, int foregroundRbg, float foregroundOpacity) {
        Color background = new Color(backgroundRgb);
        Color foreground = new Color(foregroundRbg);
        return new Color(
            Math.round(background.getRed() * (1 - foregroundOpacity) + foreground.getRed() * foregroundOpacity),
            Math.round(background.getGreen() * (1 - foregroundOpacity) + foreground.getGreen() * foregroundOpacity),
            Math.round(background.getBlue() * (1 - foregroundOpacity) + foreground.getBlue() * foregroundOpacity)
        );
    }

    /**
     * Returns if the action key is held by the user for the given event. The action key is defined as the meta key on mac, and control on
     * other platforms.
     */
    public static boolean isActionKeyDown(InputEvent event) {
        return Platform.current().os().isMac() ? event.isMetaDown() : event.isControlDown();
    }

    /**
     * Returns the action mask for the current platform.<br> On mac it's {@link InputEvent#META_DOWN_MASK} everything else is
     * {@link InputEvent#CTRL_DOWN_MASK}.
     */
    public static int getActionMask() {
        return Platform.current().os().isMac() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
    }

    /**
     * returns the action mask text for the current platform. On mac, we try to display the unicode char for cmd button.
     */
    public static String getActionKeyText() {
        if (Platform.current().os().isMac()) {
            Font labelFont = UIUtil.getLabelFont();
            return labelFont != null && labelFont.canDisplayUpTo(MacKeymapUtil.COMMAND) == -1 ? MacKeymapUtil.COMMAND : "Cmd";
        }
        return "Ctrl";
    }

    /**
     * Returns a separator that is vertically centered. It has a consistent size among Mac and Linux platforms, as {@link JSeparator} on
     * different platforms has different UI and different sizes.
     */
    public static JComponent createHorizontalSeparator() {
        JPanel separatorWrapper = new JPanel(new TabularLayout("*", "*,Fit,*"));
        separatorWrapper.add(new JSeparator(), new TabularLayout.Constraint(1, 0));
        Dimension size = new Dimension(1, 2);
        separatorWrapper.setMinimumSize(size);
        separatorWrapper.setPreferredSize(size);
        separatorWrapper.setOpaque(false);
        return separatorWrapper;
    }

    /**
     * Traverses up to the TooltipLayeredPane and sets the cursor on it.
     * <p>
     * Returns the TooltipLayeredPane if found. Null otherwise.
     */
    public static @Nullable Container setTooltipCursor(Container container, Cursor cursor) {
        Container p = container;
        while (p != null) {
            if (p instanceof TooltipLayeredPane) {
                p.setCursor(cursor);
                break;
            }
            p = p.getParent();
        }
        return p;
    }

    /**
     * Returns all the child components of container recursively.
     */
    public static Stream<Component> allComponents(Container container) {
        return Arrays.stream(container.getComponents()).flatMap(it -> {
            if (it instanceof Container child && child.getComponentCount() != 0) {
                return Stream.concat(Stream.of(it), allComponents(child));
            }
            return Stream.of(it);
        });
    }
}
