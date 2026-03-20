/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.codeEditor.impl.internal.textEditor;

import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingGroup;
import consulo.codeEditor.FoldingModel;
import consulo.codeEditor.event.FoldingListener;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 11/2/11 6:13 PM
 */
public class TextComponentFoldingModel implements FoldingModel {

    @Override
    public FoldRegion addFoldRegion(int startOffset, int endOffset, String placeholderText) {
        return null;
    }

    @Override
    public boolean addFoldRegion(FoldRegion region) {
        return false;
    }

    @Override
    public void removeFoldRegion(FoldRegion region) {
    }

    
    @Override
    public FoldRegion[] getAllFoldRegions() {
        return FoldRegion.EMPTY_ARRAY;
    }

    @Override
    public boolean isOffsetCollapsed(int offset) {
        return false;
    }

    @Override
    public FoldRegion getCollapsedRegionAtOffset(int offset) {
        return null;
    }

    @Override
    public @Nullable FoldRegion getFoldRegion(int startOffset, int endOffset) {
        return null;
    }

    @Override
    public void runBatchFoldingOperation(Runnable operation) {
    }

    @Override
    public void runBatchFoldingOperation(Runnable operation, boolean moveCaretFromCollapsedRegion) {
    }

    @Override
    public void runBatchFoldingOperation(Runnable operation, boolean dontCollapseCaret, boolean moveCaret) {
    }

    @Override
    public void runBatchFoldingOperationDoNotCollapseCaret(Runnable operation) {
    }

    @Override
    public void setFoldingEnabled(boolean isEnabled) {

    }

    @Override
    public boolean isFoldingEnabled() {
        return false;
    }

    @Override
    public FoldRegion getFoldingPlaceholderAt(Point p) {
        return null;
    }

    @Override
    public boolean intersectsRegion(int startOffset, int endOffset) {
        return false;
    }

    @Override
    public int getLastCollapsedRegionBefore(int offset) {
        return 0;
    }

    @Override
    public TextAttributes getPlaceholderAttributes() {
        return null;
    }

    @Override
    public FoldRegion[] fetchTopLevel() {
        return new FoldRegion[0];
    }

    @Override
    public @Nullable FoldRegion createFoldRegion(int startOffset, int endOffset, String placeholder, @Nullable FoldingGroup group, boolean neverExpands) {
        return null;
    }

    @Override
    public void addListener(FoldingListener listener, Disposable parentDisposable) {

    }

    @Override
    public void clearFoldRegions() {

    }

    @Override
    public void rebuild() {

    }

    
    @Override
    public List<FoldRegion> getGroupedRegions(FoldingGroup group) {
        return List.of();
    }

    @Override
    public void clearDocumentRangesModificationStatus() {

    }

    @Override
    public boolean hasDocumentRegionChangedFor(FoldRegion region) {
        return false;
    }
}
