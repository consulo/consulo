// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.codeEditor.Caret;
import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.impl.EditorScrollingPositionKeeper;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Owns the {@link DocRenderItem}s of an editor, and applies the items produced by {@link DocRenderPassFactory} to it.
 */
public class DocRenderItemManager {
    private static final Key<List<DocRenderItemImpl>> ITEMS = Key.create("doc.render.items");

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
        setItemsToEditor(editor, new DocRenderPassFactory.Items(), false);
    }

    /**
     * Updates the items of the editor to match the passed set, collapsing new ones when requested. Items which are already
     * present in the editor are kept as is (preserving their rendered/expanded state), only their text is updated when needed.
     */
    @RequiredUIAccess
    public void setItemsToEditor(Editor editor, DocRenderPassFactory.Items itemsToSet, boolean collapseNewItems) {
        if (editor.getUserData(ITEMS) == null && itemsToSet.isEmpty()) {
            return;
        }
        List<DocRenderItemImpl> items = editor.getUserData(ITEMS);
        if (items == null) {
            items = new ArrayList<>();
            editor.putUserData(ITEMS, items);
        }

        List<DocRenderItemImpl> theItems = items;
        keepScrollingPositionWhile(editor, () -> {
            List<Runnable> foldingTasks = new ArrayList<>();
            List<DocRenderItemImpl> itemsToUpdateRenderers = new ArrayList<>();
            List<String> itemsToUpdateText = new ArrayList<>();
            boolean updated = false;

            for (Iterator<DocRenderItemImpl> it = theItems.iterator(); it.hasNext(); ) {
                DocRenderItemImpl existingItem = it.next();
                DocRenderPassFactory.Item matchingNewItem =
                    existingItem.isValid() ? itemsToSet.removeItem(existingItem.getHighlighter()) : null;
                if (matchingNewItem == null) {
                    updated |= existingItem.remove(foldingTasks);
                    it.remove();
                }
                else if (matchingNewItem.textToRender != null
                    && !matchingNewItem.textToRender.equals(existingItem.getTextToRender())) {
                    itemsToUpdateRenderers.add(existingItem);
                    itemsToUpdateText.add(matchingNewItem.textToRender);
                }
                else {
                    existingItem.updateIcon(foldingTasks);
                }
            }

            Collection<DocRenderItemImpl> newRenderItems = new ArrayList<>();
            for (DocRenderPassFactory.Item item : itemsToSet) {
                DocRenderItemImpl newItem =
                    new DocRenderItemImpl(editor, item.textRange, collapseNewItems ? null : item.textToRender);
                newRenderItems.add(newItem);
                if (collapseNewItems) {
                    updated |= newItem.toggle(foldingTasks);
                    itemsToUpdateRenderers.add(newItem);
                    itemsToUpdateText.add(item.textToRender);
                }
            }

            runFoldingTasks(editor, foldingTasks);

            for (int i = 0; i < itemsToUpdateRenderers.size(); i++) {
                itemsToUpdateRenderers.get(i).setTextToRender(itemsToUpdateText.get(i));
            }
            DocRenderItemUpdater.updateRenderers(itemsToUpdateRenderers, true);

            theItems.addAll(newRenderItems);
            return updated;
        });

        updateListeners(editor, items.isEmpty());
    }

    private static void keepScrollingPositionWhile(Editor editor, BooleanSupplier task) {
        EditorScrollingPositionKeeper keeper = new EditorScrollingPositionKeeper(editor);
        keeper.savePosition();
        if (task.getAsBoolean()) {
            keeper.restorePosition(false);
        }
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
            editor.getCaretModel().addCaretListener(MyCaretListener.INSTANCE, disposable);
            Disposer.register(disposable, () -> getInstance().removeAllItems(editor));
        }
    }

    private static final class MyCaretListener implements CaretListener {
        private static final MyCaretListener INSTANCE = new MyCaretListener();

        @Override
        public void caretPositionChanged(CaretEvent event) {
            onCaretUpdate(event);
        }

        @Override
        public void caretAdded(CaretEvent event) {
            onCaretUpdate(event);
        }

        @RequiredUIAccess
        private static void onCaretUpdate(CaretEvent event) {
            Caret caret = event.getCaret();
            if (caret == null) {
                return;
            }
            int caretOffset = caret.getOffset();
            FoldRegion foldRegion = caret.getEditor().getFoldingModel().getCollapsedRegionAtOffset(caretOffset);
            if (foldRegion instanceof CustomFoldRegion customFoldRegion && caretOffset > foldRegion.getStartOffset()
                && customFoldRegion.getRenderer() instanceof DocRenderer renderer) {
                renderer.getItem().toggle();
            }
        }
    }

    @RequiredUIAccess
    public void resetToDefaultState(Editor editor) {
        Collection<DocRenderItemImpl> items = getItems(editor);
        if (items == null) {
            return;
        }
        boolean enabled = DocRenderManager.isDocRenderingEnabled(editor);
        keepScrollingPositionWhile(editor, () -> {
            List<Runnable> foldingTasks = new ArrayList<>();
            boolean updated = false;
            for (DocRenderItemImpl item : items) {
                if (!item.isValid()) {
                    continue;
                }
                boolean rendered = item.getFoldRegion() != null;
                if (rendered != enabled) {
                    updated |= item.toggle(foldingTasks);
                }
            }
            runFoldingTasks(editor, foldingTasks);
            return updated;
        });
    }

    private static void runFoldingTasks(Editor editor, List<Runnable> foldingTasks) {
        if (foldingTasks.isEmpty()) {
            return;
        }
        editor.getFoldingModel().runBatchFoldingOperation(() -> foldingTasks.forEach(Runnable::run), true, false);
    }
}
