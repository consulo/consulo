// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.internal.stickyLine;

import consulo.codeEditor.markup.MarkupModel;
import consulo.document.Document;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Represents a storage of sticky lines.
 * In general, sticky lines come from BreadcrumbsProvider by highlighting pass which puts them into the model.
 * On the other hand, the editor gets sticky lines from the model painting them on the sticky lines panel.
 * The current implementation relies on MarkupModel, but that may be changed in the future.
 */
public interface StickyLinesModel {
    @Nullable
    static  StickyLinesModel getModel(@Nonnull Project project, @Nonnull Document document) {
        return StickyLinesModelImpl.getModel(project, document);
    }

    static @Nonnull StickyLinesModel getModel(@Nonnull MarkupModel markupModel) {
        return StickyLinesModelImpl.getModel(markupModel);
    }

    default @Nonnull StickyLine addStickyLine(int startOffset, int endOffset, @Nullable String debugText) {
        return addStickyLine(SourceID.IJ, startOffset, endOffset, debugText);
    }

    @Nonnull
    StickyLine addStickyLine(@Nonnull SourceID source, int startOffset, int endOffset, @Nullable String debugText);

    void removeStickyLine(@Nonnull StickyLine stickyLine);

    void processStickyLines(int startOffset, int endOffset, @Nonnull Predicate<? super StickyLine> processor);

    void processStickyLines(@Nonnull SourceID source, @Nonnull Predicate<? super StickyLine> processor);

    @Nonnull
    List<StickyLine> getAllStickyLines();

    void removeAllStickyLines(@Nullable Project project);

    void addListener(@Nonnull Listener listener);

    void removeListener(@Nonnull Listener listener);

    void notifyLinesUpdate();

    /**
     * Marker associated with sticky line to distinguish the highlighting daemon which produced the line.
     * Source id allows us to have two separated producers(daemon) of sticky lines.
     * That's needed because it is impossible to implement BreadcrumbsProvider from Rider's side as it does the other clients.
     * In this way, one editor can paint sticky lines from both sources at the same time.
     */
    enum SourceID {
        /**
         * Indicates that the sticky line is produced by IJ's highlighting pass.
         */
        IJ,
        /**
         * Indicates that the sticky line is produced by Rider's highlighting pass.
         */
        RIDER,
    }

    interface Listener {
        /**
         * Called when a batch of sticky lines is added or removed
         */
        void linesUpdated();

        /**
         * Called when all sticky lines are removed
         */
        void linesRemoved();
    }
}
