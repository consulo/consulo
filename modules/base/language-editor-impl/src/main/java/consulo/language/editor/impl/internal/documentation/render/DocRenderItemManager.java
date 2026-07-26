// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Owns the {@link DocRenderItem}s of an editor, and applies the items produced by {@link DocRenderPassFactory} to it.
 */
public class DocRenderItemManager {
    private static final Key<Collection<DocRenderItemImpl>> ITEMS = Key.create("doc.render.items");

    private static final DocRenderItemManager ourInstance = new DocRenderItemManager();

    public static DocRenderItemManager getInstance() {
        return ourInstance;
    }

    public @Nullable Collection<DocRenderItemImpl> getItems(Editor editor) {
        return editor.getUserData(ITEMS);
    }

    public @Nullable DocRenderItem getItemAroundOffset(Editor editor, int offset) {
        Collection<DocRenderItemImpl> items = getItems(editor);
        if (items == null) {
            return null;
        }
        for (DocRenderItemImpl item : items) {
            if (!item.isValid()) {
                continue;
            }
            int start = item.getHighlighter().getStartOffset();
            int end = item.getHighlighter().getEndOffset();
            if (offset >= start && offset <= end) {
                return item;
            }
        }
        return null;
    }

    @RequiredUIAccess
    public void removeAllItems(Editor editor) {
        Collection<DocRenderItemImpl> items = getItems(editor);
        if (items == null) {
            return;
        }
        List<Runnable> foldingTasks = new ArrayList<>();
        for (DocRenderItemImpl item : items) {
            item.remove(foldingTasks);
        }
        runFoldingTasks(editor, foldingTasks);
        editor.putUserData(ITEMS, null);
    }

    /**
     * Re-creates the items of the editor from the passed set, collapsing new ones when requested.
     */
    @RequiredUIAccess
    public void setItemsToEditor(Editor editor, DocRenderPassFactory.Items itemsToSet, boolean collapseNewItems) {
        removeAllItems(editor);

        Collection<DocRenderItemImpl> items = new ArrayList<>();
        List<Runnable> foldingTasks = new ArrayList<>();
        for (DocRenderPassFactory.Item item : itemsToSet) {
            DocRenderItemImpl renderItem = new DocRenderItemImpl(editor, item.textRange, item.textToRender);
            items.add(renderItem);
            if (collapseNewItems && item.textToRender != null) {
                renderItem.toggle(foldingTasks);
            }
        }
        runFoldingTasks(editor, foldingTasks);
        editor.putUserData(ITEMS, items);
        updateListeners(editor, items.isEmpty());
    }

    private static void updateListeners(Editor editor, boolean disable) {
        if (disable) {
            DocRenderItemUpdaterListeners.disposeListeners(editor);
        }
        else {
            Disposable disposable = DocRenderItemUpdaterListeners.setupListeners(editor);
            if (disposable == null) {
                return;
            }
            Disposer.register(disposable, () -> getInstance().removeAllItems(editor));
        }
    }

    @RequiredUIAccess
    public void resetToDefaultState(Editor editor) {
        Collection<DocRenderItemImpl> items = getItems(editor);
        if (items == null) {
            return;
        }
        boolean enabled = DocRenderManager.isDocRenderingEnabled(editor);
        List<Runnable> foldingTasks = new ArrayList<>();
        for (DocRenderItemImpl item : items) {
            if (!item.isValid()) {
                continue;
            }
            boolean rendered = item.getFoldRegion() != null;
            if (rendered != enabled) {
                item.toggle(foldingTasks);
            }
        }
        runFoldingTasks(editor, foldingTasks);
    }

    private static void runFoldingTasks(Editor editor, List<Runnable> foldingTasks) {
        if (foldingTasks.isEmpty()) {
            return;
        }
        editor.getFoldingModel().runBatchFoldingOperation(() -> foldingTasks.forEach(Runnable::run), true, false);
    }
}
