/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.internal;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Tracker state can be updated without taking an Application write-lock.
 * So Application read-lock does not guarantee that two {@link #getRanges()} calls will return the same
 * results. Use {@link #readLock(Callable)} when consistency across multiple calls is needed.
 *
 * @see #isValid()
 * @see #isOperational()
 */
public interface LineStatusTrackerI {
    // -------------------------------------------------------------------------
    // State queries
    // -------------------------------------------------------------------------

    /**
     * Whether {@link #getVcsDocument()} content is successfully loaded and the tracker is not released.
     */
    boolean isOperational();

    /**
     * Whether internal state is synchronized with both documents.
     * Returns {@code false} if the tracker is not operational, or is currently frozen ({@link #doFrozen}).
     */
    boolean isValid();

    /**
     * Whether this tracker has been permanently released and must no longer be used.
     */
    boolean isReleased();

    // -------------------------------------------------------------------------
    // Documents
    // -------------------------------------------------------------------------

    Document getDocument();

    Document getVcsDocument();

    @Nullable VirtualFile getVirtualFile();

    @Nullable Project getProject();

    // -------------------------------------------------------------------------
    // Range access
    // -------------------------------------------------------------------------

    /**
     * Returns a snapshot of all changed-line ranges, or {@code null} if the tracker is not valid.
     * Requires an Application read-lock for stable results across multiple calls.
     */
    @Nullable List<VcsRange> getRanges();

    /** Returns ranges that overlap the given set of line numbers, or {@code null} if not valid. */
    @Nullable List<VcsRange> getRangesForLines(BitSet lines);

    /** Returns the range covering the given line, or {@code null} if none / not valid. */
    @Nullable VcsRange getRangeForLine(int line);

    /**
     * Finds the live range that occupies the same line positions as {@code range}.
     * Useful for re-acquiring a range after a reinstall cycle.
     */
    @Nullable VcsRange findRange(VcsRange range);

    @Nullable VcsRange getNextRange(VcsRange range);

    @Nullable VcsRange getPrevRange(VcsRange range);

    @Nullable VcsRange getNextRange(int line);

    @Nullable VcsRange getPrevRange(int line);

    // -------------------------------------------------------------------------
    // Content access
    // -------------------------------------------------------------------------

    CharSequence getVcsContent(VcsRange range);

    TextRange getVcsTextRange(VcsRange range);

    TextRange getCurrentTextRange(VcsRange range);

    // -------------------------------------------------------------------------
    // Line number translation
    // -------------------------------------------------------------------------

    int transferLineToVcs(int line, boolean approximate);

    int transferLineToFromVcs(int line, boolean approximate);

    boolean isLineModified(int line);

    boolean isRangeModified(int line1, int line2);

    // -------------------------------------------------------------------------
    // Freeze / lock
    // -------------------------------------------------------------------------

    /**
     * Prevents the internal tracker state from being updated for the duration of {@code task}.
     * The state will be reconciled once when the task finishes.
     */
    void doFrozen(Runnable task);

    /**
     * Runs {@code task} under the tracker's own internal lock.
     * The task must NOT acquire an Application read-lock inside.
     */
    <T> T readLock(Callable<T> task);

    // -------------------------------------------------------------------------
    // Listeners
    // -------------------------------------------------------------------------

    void addListener(LineStatusTrackerListener listener);

    void removeListener(LineStatusTrackerListener listener);
}
