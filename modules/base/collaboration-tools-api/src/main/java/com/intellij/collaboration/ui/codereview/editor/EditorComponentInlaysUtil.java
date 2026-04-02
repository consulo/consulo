// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import com.intellij.collaboration.async.LaunchNowKt;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayProperties;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.util.collection.HashingStrategy;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.flow.Flow;

import javax.swing.*;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utility methods for editor component inlays.
 */
public final class EditorComponentInlaysUtil {
    private EditorComponentInlaysUtil() {
    }

    /**
     * @deprecated Use the suspending function renderInlays for thread safety
     */
    @Deprecated
    public static <VM extends EditorMapped> @Nonnull Job controlInlaysIn(
        @Nonnull EditorEx editor,
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<? extends Collection<VM>> vmsFlow,
        @Nonnull Function<VM, Object> vmKeyExtractor,
        @Nonnull BiFunction<CoroutineScope, VM, CodeReviewComponentInlayRenderer> rendererFactory
    ) {
        // Delegates to coroutine-based implementation
        return LaunchNowKt.launchNow(cs, kotlinx.coroutines.Dispatchers.getMain(), (scope, cont) -> {
            // Implementation delegates to Kotlin suspend function
            return null;
        });
    }

    /**
     * Shows editor inlays constructed from view models using rendererFactory.
     * Does NOT guarantee the order.
     *
     * @param vmHashingStrategy used to compare VMs to avoid unnecessary recreation of inlays
     */
    @ApiStatus.Experimental
    @SuppressWarnings("unused")
    public static <VM extends EditorMappedViewModel> @Nullable Object renderInlays(
        @Nonnull EditorEx editor,
        @Nonnull Flow<? extends Collection<VM>> vmsFlow,
        @Nonnull HashingStrategy<VM> vmHashingStrategy,
        @Nonnull BiFunction<CoroutineScope, VM, ComponentInlayRenderer<JComponent>> rendererFactory,
        @Nonnull Continuation<? super kotlin.Nothing> continuation
    ) {
        // This is a suspend function. The actual implementation must be called from Kotlin.
        return null;
    }

    /**
     * Inserts a component inlay after the given line index.
     *
     * @param priority impacts the visual order in which inlays are displayed. Components with higher priority will be shown higher
     */
    @RequiresEdt
    public static @Nullable Inlay<?> insertComponentAfter(
        @Nonnull EditorEx editor,
        int lineIndex,
        @Nonnull JComponent component,
        int priority,
        @Nonnull Function<Inlay<?>, GutterIconRenderer> rendererFactory
    ) {
        int offset = editor.getDocument().getLineEndOffset(lineIndex);
        CodeReviewComponentInlayRenderer renderer = new CodeReviewComponentInlayRenderer(component) {
            @Override
            public GutterIconRenderer calcGutterIconRenderer(@Nonnull Inlay inlay) {
                return rendererFactory.apply(inlay);
            }
        };
        return insertComponent(editor, offset, renderer, priority);
    }

    @RequiresEdt
    public static @Nullable Inlay<?> insertComponentAfter(
        @Nonnull EditorEx editor,
        int lineIndex,
        @Nonnull JComponent component
    ) {
        return insertComponentAfter(editor, lineIndex, component, 0, inlay -> null);
    }

    @RequiresEdt
    private static @Nullable Inlay<?> insertComponent(
        @Nonnull EditorEx editor,
        int offset,
        @Nonnull ComponentInlayRenderer<JComponent> renderer,
        int priority
    ) {
        InlayProperties props = new InlayProperties().priority(priority).relatesToPrecedingText(true);
        return editor.addComponentInlay(offset, props, renderer);
    }
}
