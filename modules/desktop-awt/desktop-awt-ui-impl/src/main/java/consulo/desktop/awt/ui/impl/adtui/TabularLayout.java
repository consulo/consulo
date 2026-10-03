/*
 * Copyright (C) 2016 The Android Open Source Project
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
package consulo.desktop.awt.ui.impl.adtui;

import consulo.ui.ex.awt.JBUI;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A layout manager which makes it easy to define components that conform to a table-like layout.
 * <p>
 * An ideal use-case for this layout is a table with a fixed number of columns and a dynamic number of rows, for example a list of
 * "label/value" pairs where the columns line up neatly.
 * <p>
 * Unlike {@link GridBagLayout}, which requires setting complex constraints and hard to reason about weights, {@link TabularLayout} works by
 * setting column definitions up front. A column can be fixed, fit-to-size, or proportional. The fit-to-size calculation uses a component's
 * minimum size, not preferred size, which may be useful to keep in mind.
 * <p>
 * When a layout is requested, fixed and fit-to-width columns are calculated first, and all remaining space is split by the proportional
 * columns. Rows, in contrast, can be added on the fly. By default, they are always fit-to-height (although a vertical gap can be
 * specified), but they can be configured for sizing as well. To set optional row definitions, use the {@link #setRowSizing} method.
 * <p>
 * When you register components with a panel using this layout, you must associate it with a {@link TabularLayout.Constraint} telling it
 * which row and column it should fit within. You should usually associate one element per cell, and (unless that cell is sized to fit) the
 * element will be stretched to fully contain the cell. You can additionally specify an element that spans across multiple columns, but be
 * aware that such elements are skipped when calculating the layout. It's also worth noting that invisible components are skipped over when
 * performing the layout.
 * <p>
 * Columns are pre-allocated, so it is an error to specify a cell whose column index is out of bounds. However, rows are unbounded - you can
 * add a component at row 0 and then another at row 1000. Still, if a row doesn't have any components inside of it, it will simply be
 * skipped during layout (i.e. sparse layouts are collapsed).
 * <p>
 * A note on thread safety: This class in NOT thread safe. It should be created and accessed only from the EDT. Attempts to do so otherwise
 * will result in an assertion error, or, if assertions are stripped in production, the class will usually work fine but with a slight
 * chance of hitting a ConcurrentModificationException.
 */
public class TabularLayout implements LayoutManager2 {
    private final List<SizingRule> myColSizes;
    private final Map<Integer, SizingRule> myRowSizes = new HashMap<>();
    private int myVGap = 0; // Vertical gap between rows
    private final Map<Component, Constraint> myConstraints = new HashMap<>();

    /**
     * A definition for how to size a single column or row, indicating how its width or height will be calculated during a layout.
     *
     * @param value Value's meaning depends on this constraint's type
     */
    public record SizingRule(Type type, int value) {
        public enum Type {
            /**
             * Shrink this column as small as possible to perfectly fit all its contents.
             * <p>
             * For fit columns, {@link #value} corresponds to its {@link FitSizing}.
             */
            FIT,
            /**
             * Set this column to a fixed width in pixels.
             * <p>
             * For fixed columns, {@link #value} is a width in pixels.
             */
            FIXED,
            /**
             * Have this column eat up any remaining space (split with all other proportional columns).
             * <p>
             * For proportional columns, {@link #value} is an integer value which should be compared with all other proportional columns to
             * determine how much space it gets. For example, if column A is set to 1 and column B is set to 3, column A gets 25% of all
             * remaining space and column B gets 75%.
             */
            PROPORTIONAL,
        }

        public enum FitSizing {
            /**
             * Use the elements minimum size for determining spacing.
             * <p>
             * This is represented by a '-' at the end of "Fit". This is the default value for uninitialized rows/columns. TODO (b/77491599)
             * Update unassigned rows to use preferred in place of minimum
             */
            MINIMUM,
            /**
             * Use the elements preferred size for determining spacing.
             * <p>
             * This is the default value for "Fit" sizing.
             */
            PREFERRED,
        }

        /**
         * Create a {@link SizingRule} from a string value, where each value represents either a Fit, Fixed, or Proportional column.
         * <p>
         * A Fit cell is represented by the string "Fit" (or "Fit-" for using min fit sizing). A Fixed cell is represented by an integer +
         * "px" (e.g. "100px"). A Proportional cell is represented by an (optional) integer + "*" (e.g. "3*", "*").
         */
        public static SizingRule fromString(String s) {
            try {
                if (s.equals("*")) {
                    return new SizingRule(Type.PROPORTIONAL, 1);
                }
                else if (s.startsWith("Fit")) {
                    FitSizing fitSizing = s.endsWith("-") ? FitSizing.MINIMUM : FitSizing.PREFERRED;
                    return new SizingRule(Type.FIT, fitSizing.ordinal());
                }
                else if (s.endsWith("px")) {
                    return new SizingRule(Type.FIXED, Integer.parseInt(s.substring(0, s.length() - 2)));
                }
                else if (s.endsWith("*")) {
                    return new SizingRule(Type.PROPORTIONAL, Integer.parseInt(s.substring(0, s.length() - 1)));
                }
                else {
                    throw new IllegalArgumentException("Bad size value: \"" + s + "\"");
                }
            }
            catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Bad size value: \"" + s + "\"");
            }
        }
    }

    /**
     * Constraints which specify which cell the element is slotted into.
     * <p>
     * Create a constraint which can live across multiple cells. Note that components which span across multiple cells aren't included in
     * fit-to-size layout calculations.
     */
    public record Constraint(int row, int col, int rowSpan, int colSpan) {
        public Constraint {
            if (colSpan <= 0) {
                throw new IllegalArgumentException("TabularLayout column span must be greater than 0");
            }
        }

        /**
         * Create a constraint which can live across multiple columns. Note that components which span across multiple columns aren't
         * included in fit-to-width layout calculations.
         */
        public Constraint(int row, int col, int colSpan) {
            this(row, col, 1, colSpan);
        }

        public Constraint(int row, int col) {
            this(row, col, 1);
        }
    }

    public TabularLayout(SizingRule[] colSizes, SizingRule[] initialRowSizes) {
        myColSizes = List.of(colSizes);
        for (int i = 0; i < initialRowSizes.length; i++) {
            setRowSizing(i, initialRowSizes[i]);
        }
    }

    public TabularLayout(SizingRule... colSizes) {
        this(colSizes, new SizingRule[0]);
    }

    /**
     * Create a {@link TabularLayout} from a comma-delimited string of values that are valid for creating a {@link SizingRule}.
     * <p>
     * Examples:
     * - "Fit,*,*" - First cell fits to size, remaining two cells share leftover space equally
     * - "3*,*" - First cell gets 75% of space, second cell gets 25% of space
     * - "75*,25*" - Same as above
     * - "50px,*,100px" - First cell gets 50 pixels, last cell gets 100, middle gets remaining space
     *
     * @see SizingRule#fromString
     */
    public TabularLayout(String colSizesString) {
        this(parseSizingRules(colSizesString));
    }

    /**
     * Like TabularLayout(String) but also specifying initial sizing conditions for rows. Unlike cols, row sizing can be added dynamically
     * later (using {@link #setRowSizing}), but for the common case where the whole table size is known at creation time, this constructor
     * allows it to be expressed in a much more succinct manner.
     */
    public TabularLayout(String colSizesString, String initialRowSizesString) {
        this(parseSizingRules(colSizesString), parseSizingRules(initialRowSizesString));
    }

    public int getNumColumns() {
        return myColSizes.size();
    }

    public TabularLayout setVGap(int vGap) {
        myVGap = vGap;
        return this;
    }

    public TabularLayout setRowSizing(int rowIndex, SizingRule rowSize) {
        myRowSizes.put(rowIndex, rowSize);
        return this;
    }

    /**
     * @see SizingRule#fromString
     */
    public TabularLayout setRowSizing(int rowIndex, String rowSizeString) {
        return setRowSizing(rowIndex, SizingRule.fromString(rowSizeString));
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraint) {
        if (!(constraint instanceof Constraint c)) {
            throw new IllegalArgumentException(
                "Children of containers using " + getClass().getSimpleName() + " must be added with a constraint"
            );
        }

        if (c.col() + c.colSpan() > myColSizes.size()) {
            throw new IllegalArgumentException("Component added with invalid column span. col: " + c.col() + ", span: " + c.colSpan() +
                ", num cols: " + myColSizes.size());
        }

        myConstraints.put(comp, c);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0.5f;
    }

    @Override
    public void invalidateLayout(Container target) {
        // Do nothing
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        // Do nothing
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        myConstraints.remove(comp);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return getLayoutSize(parent, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return getLayoutSize(parent, false);
    }

    private Dimension getLayoutSize(Container container, boolean includeExtraSize) {
        LayoutResult result = new LayoutResult(container);

        int w = result.myColCalculator.getTotalSize(includeExtraSize);
        int h = result.myRowCalculator.getTotalSize(includeExtraSize);
        Insets insets = result.myInsets;
        return new Dimension(insets.left + insets.right + w, insets.top + insets.bottom + h);
    }

    @Override
    public void layoutContainer(Container parent) {
        // Ensure parent.getComponent access is synchronous
        assert EventQueue.isDispatchThread();

        LayoutResult result = new LayoutResult(parent);
        SizeCalculator colCalc = result.myColCalculator;
        SizeCalculator rowCalc = result.myRowCalculator;
        if (colCalc.getLength() == 0 || rowCalc.getLength() == 0) {
            return;
        }

        Insets insets = parent.getInsets();
        List<PosSize> rowBounds = rowCalc.getBounds(insets.top, parent.getHeight() - insets.bottom - insets.top);
        List<PosSize> colBounds = colCalc.getBounds(insets.left, parent.getWidth() - insets.right - insets.left);

        for (int index = 0; index < parent.getComponentCount(); index++) {
            Component comp = parent.getComponent(index);
            if (!comp.isVisible()) {
                continue;
            }
            Constraint cons = myConstraints.get(comp);

            int totalWidth = 0;
            for (int i = cons.col(); i < cons.col() + cons.colSpan(); i++) {
                totalWidth += colBounds.get(i).mySize;
            }
            int totalHeight = 0;
            for (int i = cons.row(); i < cons.row() + cons.rowSpan(); i++) {
                totalHeight += rowBounds.get(i).mySize;
            }

            PosSize c = colBounds.get(cons.col());
            PosSize r = rowBounds.get(cons.row());

            comp.setBounds(c.myPos, r.myPos, totalWidth, totalHeight);
        }
    }

    /**
     * Class responsible for calculating the final sizes of columns and rows, given an initial set of size rules and being notified of the
     * sizes of different components that occupy the table.
     * <p>
     * Note that we create a calculator for each dimension. Rows get one and columns get one.
     */
    private static class SizeCalculator {
        private final List<SizingRule> myRules;
        private final int myGap;
        private final int[] mySizes;
        private final float[] myPercentages;

        // Proportional cells can shrink to 0 if pressed, but if we were able to ask for any size we
        // preferred, we would choose a size so that all proportional columns would fit.
        private int myExtraSize = 0;

        /**
         * Creates a calculator given an initial set of sizing rules. Use {@link #notifySize} to fill it out with real values, and then call
         * {@link #getBounds} to pull out the final sizing values.
         */
        SizeCalculator(List<SizingRule> rules, int gap) {
            myRules = rules;
            myGap = gap;
            mySizes = new int[rules.size()];
            myPercentages = new float[rules.size()];

            float totalProportionalSize = 0;
            for (SizingRule rule : rules) {
                if (rule.type() == SizingRule.Type.PROPORTIONAL) {
                    totalProportionalSize += rule.value();
                }
            }

            for (int i = 0; i < rules.size(); i++) {
                SizingRule rule = rules.get(i);
                switch (rule.type()) {
                    case PROPORTIONAL -> {
                        assert totalProportionalSize > 0; // Set above
                        myPercentages[i] = rule.value() / totalProportionalSize; // e.g. "3*, *" -> "75%, 25%"
                    }
                    case FIXED -> mySizes[i] = JBUI.scale(rule.value());
                    case FIT -> {
                        // do nothing
                    }
                }
            }
        }

        /**
         * Creates a calculator given sparse rules (using them to create a full list of rules). This is useful for rows, where rows can be
         * created on the fly and are set to a default sizing rule in that case.
         *
         * @param sparseRules A mapping of indices to rules. Any missing index will be assumed to be fit-to-size.
         * @param numRules    The number of rules that we should create. If {@code sparseRules} happens to have an index even greater than
         *                    that, then {@code numRules} will be updated to contain it.
         */
        SizeCalculator(Map<Integer, SizingRule> sparseRules, int numRules, int gap) {
            this(fromSparseRules(sparseRules, numRules), gap);
        }

        /**
         * Returns the number of items in this calculator.
         */
        int getLength() {
            return mySizes.length;
        }

        /**
         * Notify this calculator of a component's size, keeping track of it if relevant.
         */
        void notifySize(int i, int size) {
            switch (myRules.get(i).type()) {
                case FIT -> mySizes[i] = Math.max(mySizes[i], size);
                // Calculate how much total leftover size would be needed to fit this cell  after it takes
                // its percentage cut.
                case PROPORTIONAL -> myExtraSize = Math.max(myExtraSize, Math.round(size / myPercentages[i]));
                case FIXED -> {
                    // do nothing
                }
            }
        }

        /**
         * Gets the dimensions for a component based on the type for the specified rule. The default sizing rule is minimum. The "Fit"
         * sizing rule uses preferred and is the only exception to this.
         */
        Dimension getComponentDimension(int i, Component c) {
            return myRules.get(i).value() == SizingRule.FitSizing.PREFERRED.ordinal() ? c.getPreferredSize() : c.getMinimumSize();
        }

        /**
         * Returns the total size of all cells as well as gaps. Call after you are finished calling {@link #notifySize}.
         *
         * @param includeExtraSize Include the size needed to make space for the proportional cells as well. Minimum size calculations
         *                         should ignore it while preferred size calculations should include it.
         */
        int getTotalSize(boolean includeExtraSize) {
            int sum = 0;
            int notZeroCount = 0;
            for (int size : mySizes) {
                if (size > 0) {
                    sum += size;
                    notZeroCount++;
                }
            }
            int gapsNeeded = Math.max(notZeroCount - 1, 0);
            return sum + myGap * gapsNeeded + (includeExtraSize ? myExtraSize : 0);
        }

        /**
         * Get a list of (pos, size) pairs, useful for setting the bounds of Swing components directly. For example, for columns with no
         * gaps, this would represent
         * <p>
         * x1 x2 x3 |----w1----|--w2--|----------w3----------|
         *
         * @param start      The initial position of the first cell.
         * @param totalSpace The total space of the parent container, used to calculate the final size of proportional columns.
         */
        List<PosSize> getBounds(int start, int totalSpace) {
            if (mySizes.length == 0) {
                return List.of();
            }

            int sizesSum = 0;
            for (int size : mySizes) {
                sizesSum += size;
            }
            int remainingSpace = totalSpace - sizesSum;
            List<PosSize> bounds = new ArrayList<>(mySizes.length);
            for (int size : mySizes) {
                PosSize posSize = new PosSize();
                posSize.mySize = size;
                bounds.add(posSize);
            }

            if (remainingSpace > 0) {
                int spaceUsed = 0;
                int lastIndex = -1; // Any rounding error adjustments we'll just do on the last cell
                for (int i = 0; i < myRules.size(); i++) {
                    if (myRules.get(i).type() == SizingRule.Type.PROPORTIONAL) {
                        bounds.get(i).mySize = Math.round(remainingSpace * myPercentages[i]);
                        spaceUsed += bounds.get(i).mySize;
                        lastIndex = i;
                    }
                }

                if (spaceUsed != remainingSpace && lastIndex >= 0) {
                    // Due to rounding error, we either didn't use all the space or we used too much. Make
                    // adjustments to the final column to account for it (otherwise, you'll get UI that
                    // jitters during resize). In practice, this should rarely be more than a couple of
                    // pixels.
                    bounds.get(lastIndex).mySize += remainingSpace - spaceUsed;
                }
            }

            int pos = start;
            for (PosSize bound : bounds) {
                if (bound.mySize > 0) {
                    bound.myPos = pos;
                    pos += bound.mySize + myGap;
                }
            }

            return bounds;
        }
    }

    /**
     * A position/size pair. For columns, this is x/width, and for rows, this is y/height.
     */
    private static class PosSize {
        int myPos;
        int mySize;
    }

    /**
     * Class which, when instantiated on a parent container, runs through all its components, calculates what the sizes of rows and columns
     * should be, and makes that data available through {@code rowCalculator} and {@code colCalculator} fields.
     */
    private class LayoutResult {
        final Insets myInsets;
        final SizeCalculator myColCalculator = new SizeCalculator(myColSizes, 0);
        final SizeCalculator myRowCalculator;

        LayoutResult(Container container) {
            // Ensure parent.getComponent access is synchronous
            assert EventQueue.isDispatchThread();

            List<Component> components = new ArrayList<>();

            int numRows = 0;

            myInsets = container.getInsets();
            for (int i = 0; i < container.getComponentCount(); i++) {
                Component component = container.getComponent(i);
                components.add(component);
                Constraint constraint = myConstraints.get(component);
                if (constraint == null) {
                    continue;
                }
                numRows = Math.max(numRows, constraint.row() + 1);
            }

            myRowCalculator = new SizeCalculator(myRowSizes, numRows, myVGap);

            for (Component component : components) {
                if (!component.isVisible()) {
                    continue;
                }
                Constraint constraint = myConstraints.get(component);
                if (constraint == null) {
                    continue;
                }
                if (constraint.colSpan() == 1) {
                    Dimension size = myColCalculator.getComponentDimension(constraint.col(), component);
                    myColCalculator.notifySize(constraint.col(), size.width);
                }
                if (constraint.rowSpan() == 1) {
                    Dimension size = myRowCalculator.getComponentDimension(constraint.row(), component);
                    myRowCalculator.notifySize(constraint.row(), size.height);
                }
            }
        }
    }

    private static SizingRule[] parseSizingRules(String colSizesString) {
        List<String> sizeStrings = new ArrayList<>(Arrays.asList(colSizesString.split(",")));
        while (!sizeStrings.isEmpty() && sizeStrings.get(sizeStrings.size() - 1).isEmpty()) {
            sizeStrings.remove(sizeStrings.size() - 1);
        }
        SizingRule[] rules = new SizingRule[sizeStrings.size()];
        for (int i = 0; i < rules.length; i++) {
            rules[i] = SizingRule.fromString(sizeStrings.get(i));
        }
        return rules;
    }

    private static List<SizingRule> fromSparseRules(Map<Integer, SizingRule> sparseRules, int rulesCount) {
        int additionalRulesCount = 0;
        for (Integer key : sparseRules.keySet()) {
            if (key >= rulesCount) {
                additionalRulesCount++;
            }
        }
        List<SizingRule> rules = new ArrayList<>();
        for (int i = 0; i < rulesCount + additionalRulesCount; i++) {
            // TODO (b/77491599) Update unassigned rows to use preferred in place of minimum
            SizingRule rule = sparseRules.get(i);
            rules.add(rule != null ? rule : new SizingRule(SizingRule.Type.FIT, SizingRule.FitSizing.MINIMUM.ordinal()));
        }
        return rules;
    }
}
