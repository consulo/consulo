// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.application.Application;
import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.Editor;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.impl.EditorScrollingPositionKeeper;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DocRenderItemUpdater implements Runnable {
    private static final long MAX_UPDATE_DURATION_MS = 50;

    private static final DocRenderItemUpdater ourInstance = new DocRenderItemUpdater();

    private final Map<CustomFoldRegion, Boolean> myQueue = new HashMap<>();

    static DocRenderItemUpdater getInstance() {
        return ourInstance;
    }

    @RequiredUIAccess
    public static void updateRenderers(Collection<? extends DocRenderItem> items, boolean recreateContent) {
        List<CustomFoldRegion> foldRegions = new ArrayList<>(items.size());
        for (DocRenderItem item : items) {
            CustomFoldRegion foldRegion = item.getFoldRegion();
            if (foldRegion != null) {
                foldRegions.add(foldRegion);
            }
        }
        getInstance().updateFoldRegions(foldRegions, recreateContent);
    }

    @RequiredUIAccess
    public static void updateRenderers(Editor editor, boolean recreateContent) {
        if (recreateContent) {
            DocRenderer.clearCachedLoadingPane(editor);
        }
        Collection<? extends DocRenderItem> items = DocRenderItemManager.getInstance().getItems(editor);
        if (items == null) {
            return;
        }
        updateRenderers(items, recreateContent);
    }

    @RequiredUIAccess
    void updateFoldRegions(Collection<? extends CustomFoldRegion> foldRegions, boolean recreateContent) {
        if (foldRegions.isEmpty()) {
            return;
        }
        boolean wasEmpty = myQueue.isEmpty();
        for (CustomFoldRegion foldRegion : foldRegions) {
            myQueue.merge(foldRegion, recreateContent, Boolean::logicalOr);
        }
        if (wasEmpty) {
            processChunk();
        }
    }

    @Override
    public void run() {
        processChunk();
    }

    @RequiredUIAccess
    private void processChunk() {
        long deadline = System.currentTimeMillis() + MAX_UPDATE_DURATION_MS;
        Map<Editor, EditorScrollingPositionKeeper> keepers = new HashMap<>();
        // This is a heuristic to lessen visual 'jumping' on editor opening. We'd like regions visible at target opening location to be updated
        // first, and all the rest - later. We're not specifically optimizing for the case when multiple editors are opened simultaneously now,
        // opening several editors in succession should work fine with this logic though (by the time a new editor is opened, 'high-priority'
        // regions from the previous editor are likely to have been processed already).
        List<CustomFoldRegion> toProcess = new ArrayList<>(myQueue.keySet());
        Map<Editor, Integer> memoMap = new HashMap<>();
        toProcess.sort(Comparator.comparingInt(i -> -Math.abs(i.getStartOffset() - getVisibleOffset(i.getEditor(), memoMap))));
        Map<Editor, List<Runnable>> editorTasks = new HashMap<>();
        do {
            CustomFoldRegion region = toProcess.remove(toProcess.size() - 1);
            boolean updateContent = myQueue.remove(region);
            if (region.isValid()) {
                Editor editor = region.getEditor();
                keepers.computeIfAbsent(editor, e -> {
                    EditorScrollingPositionKeeper keeper = new EditorScrollingPositionKeeper(editor);
                    keeper.savePosition();
                    return keeper;
                });
                List<Runnable> tasks = editorTasks.computeIfAbsent(editor, e -> new ArrayList<>());
                if (region.getRenderer() instanceof DocRenderer renderer) {
                    renderer.update(true, updateContent, tasks);
                }
                if (tasks.size() > 20) {
                    runFoldingTasks(editor, tasks);
                }
            }
        }
        while (!toProcess.isEmpty() && System.currentTimeMillis() < deadline);
        editorTasks.forEach(DocRenderItemUpdater::runFoldingTasks);
        keepers.values().forEach(k -> k.restorePosition(false));
        if (!myQueue.isEmpty()) {
            Application.get().invokeLater(this::processChunk);
        }
    }

    private static void runFoldingTasks(Editor editor, List<Runnable> tasks) {
        editor.getFoldingModel().runBatchFoldingOperation(() -> tasks.forEach(Runnable::run), true, false);
        tasks.clear();
    }

    private static int getVisibleOffset(Editor editor, Map<Editor, Integer> memoMap) {
        return memoMap.computeIfAbsent(editor, e -> {
            Rectangle visibleArea = e.getScrollingModel().getVisibleAreaOnScrollingFinished();
            if (editor.isDisposed() || visibleArea.height <= 0) {
                return e.getCaretModel().getOffset();
            }
            else {
                int y = visibleArea.y + visibleArea.height / 2;
                int visualLine = e.yToVisualLine(y);
                return e.visualPositionToOffset(new VisualPosition(visualLine, 0));
            }
        });
    }
}
