// Copyright 2013-2026 consulo.io
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package consulo.codeEditor.imaginary;

import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingGroup;
import consulo.codeEditor.FoldingModel;
import consulo.codeEditor.event.FoldingListener;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

import org.jspecify.annotations.Nullable;
import java.awt.*;
import java.util.List;

/**
 * Imaginary {@link FoldingModel} implementation that captures collapsed regions from
 * a real editor at construction time. Only {@link #isOffsetCollapsed(int)} and
 * {@link #getCollapsedRegionAtOffset(int)} are functional; all other mutating or structural
 * methods throw {@link UnsupportedOperationException}.
 *
 * @author VISTALL
 * @since 2026-03-27
 */
class ImaginaryFoldingModel implements FoldingModel {
    // Collapsed (non-expanded) regions captured at construction time.
    private final FoldRegion[] myCollapsedRegions;

    ImaginaryFoldingModel(FoldRegion[] collapsedRegions) {
        myCollapsedRegions = collapsedRegions;
    }

    static ImaginaryFoldingModel create(FoldingModel foldingModel) {
        FoldRegion[] allRegions = foldingModel.getAllFoldRegions();
        int count = 0;
        for (FoldRegion region : allRegions) {
            if (!region.isExpanded()) {
                count++;
            }
        }
        FoldRegion[] collapsed = new FoldRegion[count];
        int i = 0;
        for (FoldRegion region : allRegions) {
            if (!region.isExpanded()) {
                collapsed[i++] = region;
            }
        }
        return new ImaginaryFoldingModel(collapsed);
    }

    @Override
    public boolean isOffsetCollapsed(int offset) {
        return getCollapsedRegionAtOffset(offset) != null;
    }

    @Nullable
    @Override
    public FoldRegion addFoldRegion(int startOffset, int endOffset, String placeholderText) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFoldRegion(FoldRegion region) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FoldRegion[] getAllFoldRegions() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public FoldRegion getCollapsedRegionAtOffset(int offset) {
        FoldRegion outermost = null;
        for (FoldRegion region : myCollapsedRegions) {
            if (region.getStartOffset() <= offset && offset < region.getEndOffset()) {
                if (outermost == null || region.getStartOffset() < outermost.getStartOffset()) {
                    outermost = region;
                }
            }
        }
        return outermost;
    }

    @Nullable
    @Override
    public FoldRegion getFoldRegion(int startOffset, int endOffset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void runBatchFoldingOperation(@RequiredUIAccess Runnable operation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void runBatchFoldingOperation(Runnable operation, boolean moveCaretFromCollapsedRegion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void runBatchFoldingOperation(Runnable operation, boolean dontCollapseCaret, boolean moveCaret) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void runBatchFoldingOperationDoNotCollapseCaret(Runnable operation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFoldingEnabled(boolean isEnabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFoldingEnabled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FoldRegion getFoldingPlaceholderAt(Point p) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean intersectsRegion(int startOffset, int endOffset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLastCollapsedRegionBefore(int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TextAttributes getPlaceholderAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FoldRegion[] fetchTopLevel() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public FoldRegion createFoldRegion(int startOffset, int endOffset, String placeholder, @Nullable FoldingGroup group, boolean neverExpands) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(FoldingListener listener, Disposable parentDisposable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearFoldRegions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rebuild() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FoldRegion> getGroupedRegions(FoldingGroup group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearDocumentRangesModificationStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasDocumentRegionChangedFor(FoldRegion region) {
        throw new UnsupportedOperationException();
    }
}
