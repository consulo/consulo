// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.diff;

import consulo.codeEditor.EditorEx;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModelListener;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.codeEditor.util.EditorUtil;
import consulo.diff.util.LineRange;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import jakarta.annotation.Nonnull;

import java.util.Set;

@Deprecated
public class EditorRangesController {
    private final @Nonnull DiffEditorGutterIconRendererFactory gutterIconRendererFactory;
    private final @Nonnull EditorEx editor;
    private final it.unimi.dsi.fastutil.ints.IntSet commentableLines = IntSets.synchronize(new IntOpenHashSet());
    private final @Nonnull Set<RangeHighlighterEx> highlighters = ConcurrentCollectionFactory.createConcurrentSet();

    public EditorRangesController(
        @Nonnull DiffEditorGutterIconRendererFactory gutterIconRendererFactory,
        @Nonnull EditorEx editor
    ) {
        this.gutterIconRendererFactory = gutterIconRendererFactory;
        this.editor = editor;

        Disposable listenerDisposable = Disposer.newDisposable();
        editor.getMarkupModel().addMarkupModelListener(
            listenerDisposable,
            new MarkupModelListener() {
                @Override
                public void afterRemoved(@Nonnull RangeHighlighterEx highlighter) {
                    if (!(highlighter.getGutterIconRenderer() instanceof AddCommentGutterIconRenderer iconRenderer)) {
                        return;
                    }
                    Disposer.dispose(iconRenderer);
                    commentableLines.remove(iconRenderer.getLine());
                    highlighters.remove(highlighter);
                }
            }
        );
        IconVisibilityController iconVisibilityController = new IconVisibilityController(highlighters);
        editor.addEditorMouseListener(iconVisibilityController);
        editor.addEditorMouseMotionListener(iconVisibilityController);

        EditorUtil.disposeWithEditor(editor, listenerDisposable);
    }

    protected void markCommentableLines(@Nonnull LineRange range) {
        for (int i = range.start; i < range.end; i++) {
            if (!commentableLines.add(i)) {
                continue;
            }
            int start = editor.getDocument().getLineStartOffset(i);
            int end = editor.getDocument().getLineEndOffset(i);
            int line = i;
            highlighters.add(editor.getMarkupModel().addRangeHighlighterAndChangeAttributes(null,
                start,
                end,
                HighlighterLayer.LAST,
                HighlighterTargetArea.EXACT_RANGE,
                false,
                highlighter -> {
                    highlighter.setGutterIconRenderer(gutterIconRendererFactory.createCommentRenderer(line));
                }
            ));
        }
    }
}
