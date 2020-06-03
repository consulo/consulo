/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.web.internal.ex;

import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.editor.ex.FoldingListener;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.editor.markup.TextAttributes;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-10
 */
public class WebFoldingModelImpl implements FoldingModelEx {
  @Nullable
  @Override
  public FoldRegion addFoldRegion(int startOffset, int endOffset, @Nonnull String placeholderText) {
    return null;
  }

  @Override
  public void removeFoldRegion(@Nonnull FoldRegion region) {

  }

  @Nonnull
  @Override
  public FoldRegion[] getAllFoldRegions() {
    return new FoldRegion[0];
  }

  @Override
  public boolean isOffsetCollapsed(int offset) {
    return false;
  }

  @Nullable
  @Override
  public FoldRegion getCollapsedRegionAtOffset(int offset) {
    return null;
  }

  @Nullable
  @Override
  public FoldRegion getFoldRegion(int startOffset, int endOffset) {
    return null;
  }

  @Override
  public void runBatchFoldingOperation(@Nonnull Runnable operation) {

  }

  @Override
  public void runBatchFoldingOperation(@Nonnull Runnable operation, boolean moveCaretFromCollapsedRegion) {

  }

  @Override
  public void runBatchFoldingOperationDoNotCollapseCaret(@Nonnull Runnable operation) {

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

  @Nullable
  @Override
  public FoldRegion createFoldRegion(int startOffset, int endOffset, @Nonnull String placeholder, @Nullable FoldingGroup group, boolean neverExpands) {
    return null;
  }

  @Override
  public void addListener(@Nonnull FoldingListener listener, @Nonnull Disposable parentDisposable) {

  }

  @Override
  public void clearFoldRegions() {

  }

  @Override
  public void rebuild() {

  }

  @Nonnull
  @Override
  public List<FoldRegion> getGroupedRegions(FoldingGroup group) {
    return Collections.emptyList();
  }

  @Override
  public void clearDocumentRangesModificationStatus() {

  }

  @Override
  public boolean hasDocumentRegionChangedFor(@Nonnull FoldRegion region) {
    return false;
  }
}
