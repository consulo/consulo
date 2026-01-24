// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.stream.trace.*;
import consulo.execution.debug.stream.ui.PaintingListener;
import consulo.execution.debug.stream.ui.TraceContainer;
import consulo.execution.debug.stream.ui.ValuesSelectionListener;
import consulo.proxy.EventDispatcher;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.util.LightDarkColorValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static consulo.ui.ex.awt.tree.TreeUtil.collectSelectedPaths;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class CollectionTree extends XDebuggerTree implements TraceContainer {
    private static final Map<Integer, ColorValue> COLORS_CACHE = new HashMap<>();

    protected final Map<TraceElement, TreePath> myValue2Path = new HashMap<>();
    protected final Map<TreePath, TraceElement> myPath2Value = new HashMap<>();

    @SuppressWarnings("unused")
    private final String myDebugName;

    private Set<TreePath> myHighlighted = Collections.emptySet();
    private final EventDispatcher<ValuesSelectionListener> mySelectionDispatcher = EventDispatcher.create(ValuesSelectionListener.class);
    private final EventDispatcher<PaintingListener> myPaintingDispatcher = EventDispatcher.create(PaintingListener.class);

    private boolean myIgnoreInternalSelectionEvents = false;
    private boolean myIgnoreExternalSelectionEvents = false;

    protected CollectionTree(@Nonnull List<TraceElement> traceElements,
                             @Nonnull GenericEvaluationContext context,
                             @Nonnull CollectionTreeBuilder collectionTreeBuilder,
                             @Nonnull String debugName) {
        super(context.getProject(), collectionTreeBuilder.getEditorsProvider(), null, XDebuggerActions.INSPECT_TREE_POPUP_GROUP, null);

        myDebugName = debugName;

        addTreeSelectionListener(e -> {
            if (myIgnoreInternalSelectionEvents) {
                return;
            }
            List<TraceElement> selectedItems =
                collectSelectedPaths(this).stream()
                    .map(this::getTopPath)
                    .map(myPath2Value::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            fireSelectionChanged(selectedItems);
        });

        setSelectionRow(0);
        expandNodesOnLoad(node -> node == getRoot());
    }

    public static CollectionTree create(@Nullable Value streamResult,
                                        @Nonnull List<TraceElement> traceElements,
                                        @Nonnull DebuggerCommandLauncher debuggerCommandLauncher,
                                        @Nonnull GenericEvaluationContext evaluationContext,
                                        @Nonnull CollectionTreeBuilder collectionTreeBuilder,
                                        @Nonnull String debugName) {
        if (streamResult == null) {
            return new IntermediateTree(traceElements, evaluationContext, collectionTreeBuilder, debugName);
        }
        else {
            return new TerminationTree(streamResult, traceElements, debuggerCommandLauncher, evaluationContext, collectionTreeBuilder, debugName);
        }
    }

    @Override
    public boolean isFileColorsEnabled() {
        return true;
    }

    @Override
    public @Nullable ColorValue getFileColorForPath(@Nonnull TreePath path) {
        if (isPathHighlighted(path)) {
            Color background = UIUtil.getTreeSelectionBackground(true);
            return COLORS_CACHE.computeIfAbsent(background.getRGB(), rgb -> new LightDarkColorValue(
                new RGBColor(background.getRed(), background.getGreen(), background.getBlue(), 75),
                new RGBColor(background.getRed(), background.getGreen(), background.getBlue(), 100)));
        }

        return TargetAWT.from(UIUtil.getTreeBackground());
    }

    @Override
    public void clearSelection() {
        myIgnoreInternalSelectionEvents = true;
        super.clearSelection();
        myIgnoreInternalSelectionEvents = false;
    }

    public @Nullable Rectangle getRectByValue(@Nonnull TraceElement element) {
        TreePath path = myValue2Path.get(element);
        return path == null ? null : getPathBounds(path);
    }

    @Override
    public void highlight(@Nonnull List<TraceElement> elements) {
        clearSelection();

        highlightValues(elements);
        tryScrollTo(elements);

        updatePresentation();
    }

    @Override
    public void select(@Nonnull List<TraceElement> elements) {
        TreePath[] paths = elements.stream().map(myValue2Path::get).toArray(TreePath[]::new);

        select(paths);
        highlightValues(elements);

        if (paths.length > 0) {
            scrollPathToVisible(paths[0]);
        }

        updatePresentation();
    }

    @Override
    public void addSelectionListener(@Nonnull ValuesSelectionListener listener) {
        // TODO: dispose?
        mySelectionDispatcher.addListener(listener);
    }

    @Override
    public boolean highlightedExists() {
        return !isSelectionEmpty() || !myHighlighted.isEmpty();
    }

    public abstract int getItemsCount();

    public void addPaintingListener(@Nonnull PaintingListener listener) {
        myPaintingDispatcher.addListener(listener);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        myPaintingDispatcher.getMulticaster().componentPainted();
    }

    private void select(@Nonnull TreePath[] paths) {
        if (myIgnoreExternalSelectionEvents) {
            return;
        }

        myIgnoreInternalSelectionEvents = true;
        getSelectionModel().setSelectionPaths(paths);
        myIgnoreInternalSelectionEvents = false;
    }

    private void fireSelectionChanged(List<TraceElement> selectedItems) {
        myIgnoreExternalSelectionEvents = true;
        mySelectionDispatcher.getMulticaster().selectionChanged(selectedItems);
        myIgnoreExternalSelectionEvents = false;
    }

    private void tryScrollTo(@Nonnull List<TraceElement> elements) {
        int[] rows = elements.stream().map(myValue2Path::get).filter(Objects::nonNull).mapToInt(this::getRowForPath).sorted().toArray();
        if (rows.length == 0) {
            return;
        }

        if (isShowing()) {
            Rectangle bestVisibleArea = optimizeRowsCountInVisibleRect(rows);
            Rectangle visibleRect = getVisibleRect();
            boolean notVisibleHighlightedRowExists = Arrays
                .stream(rows)
                .anyMatch(x -> !visibleRect.intersects(getRowBounds(x)));
            if (notVisibleHighlightedRowExists) {
                scrollRectToVisible(bestVisibleArea);
            }
        }
        else {
            // Use slow path if component hidden
            scrollPathToVisible(getPathForRow(rows[0]));
        }
    }

    private @Nonnull Rectangle optimizeRowsCountInVisibleRect(@Nonnull int[] rows) {
        // a simple scan-line algorithm to find an optimal subset of visible rows (maximum)
        Rectangle visibleRect = getVisibleRect();
        int height = visibleRect.height;

        class Result {
            private int top = 0;
            private int bot = 0;

            @Contract(pure = true)
            private int count() {
                return bot - top;
            }
        }

        int topIndex = 0;
        int bottomIndex = 1;
        Rectangle rowBounds = getRowBounds(rows[topIndex]);
        if (rowBounds == null) {
            return visibleRect;
        }
        int topY = rowBounds.y;

        Result result = new Result();
        while (bottomIndex < rows.length) {
            int nextY = getRowBounds(rows[bottomIndex]).y;
            while (nextY - topY > height) {
                topIndex++;
                rowBounds = getRowBounds(rows[topIndex]);
                if (rowBounds == null) {
                    return visibleRect;
                }
                topY = rowBounds.y;
            }

            if (bottomIndex - topIndex > result.count()) {
                result.top = topIndex;
                result.bot = bottomIndex;
            }

            bottomIndex++;
        }

        int y = getRowBounds(rows[result.top]).y;
        if (y > visibleRect.y) {
            Rectangle botBounds = getRowBounds(rows[result.bot]);
            y = botBounds.y + botBounds.height - visibleRect.height;
        }
        return new Rectangle(visibleRect.x, y, visibleRect.width, height);
    }

    private void highlightValues(@Nonnull List<TraceElement> elements) {
        myHighlighted = elements.stream().map(myValue2Path::get).collect(Collectors.toSet());
    }

    private void updatePresentation() {
        revalidate();
        repaint();
    }

    public boolean isHighlighted(@Nonnull TraceElement traceElement) {
        TreePath path = myValue2Path.get(traceElement);
        return path != null && isPathHighlighted(path);
    }

    private boolean isPathHighlighted(@Nonnull TreePath path) {
        return myHighlighted.contains(path) || isPathSelected(path);
    }

    @Nonnull
    private TreePath getTopPath(@Nonnull TreePath path) {
        TreePath current = path;
        while (current != null && !myPath2Value.containsKey(current)) {
            current = current.getParentPath();
        }

        return current != null ? current : path;
    }
}
