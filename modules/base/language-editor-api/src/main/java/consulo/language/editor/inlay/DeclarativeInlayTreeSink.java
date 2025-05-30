// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Collects inlays during construction.
 */
public interface DeclarativeInlayTreeSink {
    /**
     * @deprecated Use {@link #addPresentation(DeclarativeInlayPosition, List, String, HintFormat, Consumer)} instead
     */
    @Deprecated
    default void addPresentation(DeclarativeInlayPosition position,
                                 List<DeclarativeInlayPayload> payloads,
                                 String tooltip,
                                 boolean hasBackground,
                                 Consumer<DeclarativePresentationTreeBuilder> builder) {
        addPresentation(position,
            payloads,
            tooltip,
            hasBackground
                ? HintFormat.DEFAULT
                : HintFormat.DEFAULT.withColorKind(HintColorKind.TextWithoutBackground),
            builder);
    }

    default void addPresentation(@Nonnull DeclarativeInlayPosition position,
                                 @Nonnull HintFormat format,
                                 @Nonnull Consumer<DeclarativePresentationTreeBuilder> builder) {
        addPresentation(position, null, null, format, builder);
    }

    /**
     * Saves presentation for later application.
     *
     * @param payloads   optional list of payloads
     * @param tooltip    optional tooltip text
     * @param hintFormat format for the hint
     * @param builder    builder for a given inlay entry; will be called in place
     */
    default void addPresentation(@Nonnull DeclarativeInlayPosition position,
                                 @Nonnull Consumer<DeclarativePresentationTreeBuilder> builder) {
        addPresentation(position, null, null, HintFormat.DEFAULT, builder);
    }

    /**
     * Saves presentation for later application.
     *
     * @param payloads   optional list of payloads
     * @param tooltip    optional tooltip text
     * @param hintFormat format for the hint
     * @param builder    builder for a given inlay entry; will be called in place
     */
    void addPresentation(@Nonnull DeclarativeInlayPosition position,
                         @Nullable List<DeclarativeInlayPayload> payloads,
                         @Nullable String tooltip,
                         @Nonnull HintFormat hintFormat,
                         @Nonnull Consumer<DeclarativePresentationTreeBuilder> builder);

    /**
     * Explicit branch, which will be executed only if given {@code optionId} is enabled.
     *
     * @param optionId the ID of the option to check
     * @param block    block of code to conditionally execute; will be called in place
     */
    void whenOptionEnabled(String optionId, Runnable block);
}