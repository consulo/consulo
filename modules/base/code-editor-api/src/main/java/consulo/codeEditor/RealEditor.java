/*
 * Copyright 2013-2022 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.codeEditor;

import consulo.codeEditor.markup.RangeHighlighter;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 06/12/2020
 * <p>
 * Base interface for real implementation for editors (not editor windows)
 */
public interface RealEditor extends EditorEx {
    Key<Boolean> DO_DOCUMENT_UPDATE_TEST = Key.create("DoDocumentUpdateTest");
    Key<Boolean> FORCED_SOFT_WRAPS = Key.create("forced.soft.wraps");
    Key<Boolean> SOFT_WRAPS_EXIST = Key.create("soft.wraps.exist");

    void throwEditorNotDisposedError(@Nonnull final String msg);

    void release();

    default int offsetToVisualLine(int offset) {
        return offsetToVisualLine(offset, false);
    }

    int offsetToVisualLine(int offset, boolean beforeSoftWrap);

    int visualLineStartOffset(int visualLine);

    default void validateSize() {
    }

    @Nonnull
    Disposable getDisposable();

    boolean isHighlighterAvailable(@Nonnull RangeHighlighter highlighter);

    boolean isScrollToCaret();

    boolean shouldSoftWrapsBeForced();

    void setHighlightingFilter(@Nullable Predicate<RangeHighlighter> filter);

    int getFontSize();

    void startDumb();

    void stopDumbLater();

    default void reinitViewSettings() {
        reinitSettings();
    }

    default int getVisibleLineCount() {
        return getDocument().getLineCount();
    }

    default boolean isInDistractionFreeMode() {
        return false;
    }

    default void updateCaretCursor() {
    }

    default boolean isRtlLocation(@Nonnull VisualPosition visualPosition) {
        return false;
    }

    default boolean isAtBidiRunBoundary(@Nonnull VisualPosition visualPosition) {
        return false;
    }

    default int findNearestDirectionBoundary(int offset, boolean lookForward) {
        return offset;
    }

    default float getScale() {
        return 1;
    }

    default void hideCursor() {
    }

    default int getDescent() {
        return 0;
    }

    default boolean isStickyLinePainting() {
        return false;
    }

    default int getStickyLinesPanelHeight() {
        return 0;
    }

    default boolean isRightAligned() {
        return false;
    }
}
