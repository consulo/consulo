/*
 * Copyright (C) 2017 The Android Open Source Project
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

import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBCurrentTheme;

import java.awt.*;

/**
 * Colors defined in the UX prototype
 */
public final class StudioColors {
    /**
     * Background color for panels that have a primary role.
     * <p>
     * Example: central panel of the layout editor.
     */
    public static final Color primaryPanelBackground = JBColor.namedColor("UIDesigner.Canvas.background", new JBColor(0xf5f5f5, 0x2D2F31));

    /**
     * Background color for panels that have a secondary role
     * <p>
     * Example: the palette or component tree in the layout editor.
     */
    public static final Color secondaryPanelBackground = JBColor.namedColor("UIDesigner.Panel.background", new JBColor(0xfcfcfc, 0x313435));

    /**
     * Color of the border that separates panels.
     * <p>
     * Example : Between the component tree and the main panel of the layout editor
     */
    public static final Color border = JBColor.namedColor("UIDesigner.Panel.borderColor", new JBColor(0xc9c9c9, 0x282828));

    /**
     * Color of the 3d lines in the transform panel of the layout editor
     */
    public static final Color lines3d = JBColor.namedColor("UIDesigner.Panel.lines3d", new JBColor(0x2D2D2D, 0x26A04A));

    /**
     * Color of the graph lines in the transition panel of the layout editor
     */
    public static final Color graphLines = JBColor.namedColor("UIDesigner.Panel.graphLines", new JBColor(0x2D2D2D, 0xD9D9D9));

    /**
     * Color of the graph lines in the transition panel of the layout editor
     */
    public static final Color secondaryGraphLines =
        JBColor.namedColor("UIDesigner.Panel.secondaryGraphLines", new JBColor(0x636363, 0x5F6265));

    /**
     * Color of the graph lines in the transition panel of the layout editor
     */
    public static final Color graphLabel = JBColor.namedColor("UIDesigner.Panel.graphLabel", new JBColor(0x636363, 0x8A8A8A));

    /**
     * Border color to use when separating element inside the same panel.
     * <p>
     * Example: border between the category list and widget list in the layout editor's palette
     */
    public static final Color borderLight = JBColor.namedColor("Canvas.Tooltip.borderColor", new JBColor(0xD9D9D9, 0x4A4A4A));

    /**
     * Background color for tooltips on canvases
     * <p>
     * Example: Hover tooltips for chart data points, tooltips on designer surfaces
     */
    public static final Color canvasTooltipBackground = JBColor.namedColor("Canvas.Tooltip.background", new JBColor(0xf7f7f7, 0x4A4C4C));

    /**
     * Background color for content (same background colors as Editors)
     * <p>
     * Example: Background for charts, editors
     */
    public static final Color primaryContentBackground = JBColor.namedColor("Content.background", new JBColor(0xffffff, 0x2b2b2b));

    /**
     * Color for textual content that is clickable.
     * <p>
     * Example: text color of "Leak" button
     */
    public static final Color linkForeground = JBCurrentTheme.Link.Foreground.ENABLED;

    /**
     * Background color for selected content.
     * <p>
     * Example: selected range in profilers.
     */
    public static final Color contentSelectionBackground =
        JBColor.namedColor("Content.selectionBackground", new JBColor(new Color(0x330478DA, true), new Color(0x4C2395F5, true)));

    /**
     * Background color for deselected content.
     * <p>
     * Example: box selection in profilers.
     */
    public static final Color contentDeselectionBackground =
        JBColor.namedColor("Content.selectionInactiveBackground", new JBColor(new Color(0x33121212, true), new Color(0x33EDEDED, true)));

    /**
     * Overlay background color for selected range
     * <p>
     * Example: box selection overlay in profilers
     */
    public static final Color selectionOverlayBackground = new JBColor(new Color(0x330478DA, true), new Color(0x4C2395F5, true));

    /**
     * Overlay background color for deselected content
     * <p>
     * Example: box selection overlay in profilers
     */
    public static final Color inactiveSelectionOverlayBackground = new JBColor(new Color(0x33121212, true), new Color(0x33EDEDED, true));

    /**
     * Background color for an active selection.
     * <p>
     * Example: selected track in a track group.
     */
    public static final Color selectionBackground = JBColor.namedColor("List.selectionBackground", new JBColor(0x4874D7, 0x1E67CE));

    /**
     * Color of the text used to display cpu capture display usage instructions.
     * <p>
     * Example: Keyboard/mouse shortcut descriptions in Summary tab of a cpu profiling capture.
     */
    public static final Color usageInstructionsText = JBColor.namedColor("Editor.foreground", new JBColor(Gray._80, Gray._160));

    /**
     * Color of deadline-missed jank event when hovered
     */
    public static final Color missedDeadlineJank = JBColor.namedColor("Profiler.missedDeadlineJank", new JBColor(0xe8515f, 0xe8515f));

    /**
     * Color of deadline-missed jank event when not hovered
     */
    public static final Color fadedMissedDeadlineJank =
        JBColor.namedColor("Profiler.fadedMissedDaedlineJank", new JBColor(0xf8cbcf, 0x553333));

    /**
     * Color of jank events other than deadline-missed when hovered
     */
    public static final Color otherJank = JBColor.namedColor("Profiler.otherJank", new JBColor(0xe1a336, 0xe1a336));

    /**
     * Color of jank events other than deadline-missed when not hovered
     */
    public static final Color fadedOtherJank = JBColor.namedColor("Profiler.fadedOtherJank", new JBColor(0xf6e3c3, 0x555533));

    /**
     * Color of good-frame event when hovered
     */
    public static final Color goodFrame = JBColor.namedColor("Profiler.otherJank", new JBColor(0x36a336, 0x36a336));

    /**
     * Color of good-frame event when not hovered
     */
    public static final Color fadedGoodFrame = JBColor.namedColor("Profiler.fadedOtherJank", new JBColor(0xc3e3c3, 0x335533));

    /**
     * Neutral color of lifecycle event when selected
     */
    public static final Color neutralLifecycleEvent =
        JBColor.namedColor("Profiler.neutralLifecycleEvent", new JBColor(Color.DARK_GRAY, Color.LIGHT_GRAY));

    /**
     * Neutral color of lifecycle event when not selected
     */
    public static final Color fadedNeutralLifecycleEvent =
        JBColor.namedColor("Profiler.neutralLifecycleEvent", new JBColor(Color.LIGHT_GRAY, Color.DARK_GRAY));

    /**
     * Default track background color
     */
    public static final Color trackBackground = JBColor.namedColor("Profiler.trackBackground", new JBColor(0xffffff, 0x323232));

    private StudioColors() {
    }
}
