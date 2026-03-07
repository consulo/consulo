// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import com.intellij.collaboration.async.CollectScopedKt;
import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.*;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;

/**
 * Utilities for showing code review inlay range outlines in the editor.
 */
@ApiStatus.Experimental
public final class CodeReviewEditorInlayRangeOutlineUtils {
    private CodeReviewEditorInlayRangeOutlineUtils() {
    }

    /**
     * Suspending method to show an inlay outline in the editor.
     */
    @SuppressWarnings("unused")
    public static @Nullable Object showInlayOutline(
        @Nonnull EditorEx editor,
        @Nonnull CodeReviewCommentableEditorModel.WithMultilineComments editorModel,
        @Nonnull CodeReviewInlayModel.Ranged inlayModel,
        @Nonnull CodeReviewComponentInlayRenderer inlayRenderer,
        @Nonnull CodeReviewActiveRangesTracker activeRangesTracker,
        @Nonnull Continuation<? super kotlin.Nothing> continuation
    ) {
        // This is a suspend function. The actual coroutine logic should be called from Kotlin.
        return null;
    }

    public static @Nonnull JComponent wrapWithDimming(
        @Nonnull JComponent component,
        @Nonnull CodeReviewInlayModel.Ranged inlayModel,
        @Nonnull CodeReviewActiveRangesTracker activeRangesTracker
    ) {
        FadeLayerUI fadeLayerUI = new FadeLayerUI();
        JLayer<JComponent> layer = new JLayer<>(component, fadeLayerUI);
        // The dimming logic requires coroutine-based collection - wired from Kotlin side
        return layer;
    }

    static @Nonnull IntRange yRangeForLogicalLineRange(@Nonnull Editor editor, int startLine, int endLine) {
        int maxLine = Math.max(editor.getDocument().getLineCount() - 1, 0);
        int startOffset = editor.getDocument().getLineStartOffset(Math.min(Math.max(startLine, 0), maxLine));
        int endOffset = editor.getDocument().getLineEndOffset(Math.min(Math.max(endLine, 0), maxLine));

        int startY = editor.offsetToXY(startOffset).y;

        CustomFoldRegion foldRegion = null;
        var collapsed = editor.getFoldingModel().getCollapsedRegionAtOffset(endOffset - 1);
        if (collapsed instanceof CustomFoldRegion cfr) {
            foldRegion = cfr;
        }

        int endY;
        if (foldRegion != null && foldRegion.getLocation() != null) {
            endY = foldRegion.getLocation().y + foldRegion.getHeightInPixels();
        }
        else {
            endY = editor.offsetToXY(endOffset).y + editor.getLineHeight();
        }

        return new IntRange(startY, endY);
    }

    private static final class FadeLayerUI extends LayerUI<JComponent> {
        private float alpha = 1f;

        void setAlpha(float a) {
            alpha = Math.max(0f, Math.min(a, 1f));
        }

        @Override
        public void paint(@Nonnull Graphics g, @Nonnull JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                super.paint(g2, c);
                g2.setComposite(old);
            }
            finally {
                g2.dispose();
            }
        }
    }

    private static final class IntRange {
        final int first;
        final int last;

        IntRange(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }
}
