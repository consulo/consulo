/*
 * Copyright 2013-2021 consulo.io
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
package consulo.web.internal.ui.editor;

import consulo.codeEditor.*;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.ui.ex.action.ActionGroup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 07/08/2021
 */
public class WebEditorGutterComponentImpl implements EditorGutterComponentEx {
  @Nullable
  @Override
  public FoldRegion findFoldingAnchorAt(int x, int y) {
    return null;
  }

  @Nonnull
  @Override
  public List<GutterMark> getGutterRenderers(int line) {
    return List.of();
  }

  @Override
  public int getWhitespaceSeparatorOffset() {
    return 0;
  }

  @Override
  public void revalidateMarkup() {

  }

  @Override
  public int getLineMarkerAreaOffset() {
    return 0;
  }

  @Override
  public int getIconAreaOffset() {
    return 0;
  }

  @Override
  public int getLineMarkerFreePaintersAreaOffset() {
    return 0;
  }

  @Override
  public int getIconsAreaWidth() {
    return 0;
  }

  @Override
  public int getAnnotationsAreaOffset() {
    return 0;
  }

  @Override
  public int getAnnotationsAreaWidth() {
    return 0;
  }

  @Nullable
  @Override
  public Point getCenterPoint(GutterIconRenderer renderer) {
    return null;
  }

  @Override
  public void setLineNumberConverter(@Nonnull LineNumberConverter primaryConverter, @Nullable LineNumberConverter additionalConverter) {

  }

  @Override
  public void setShowDefaultGutterPopup(boolean show) {

  }

  @Override
  public void setCanCloseAnnotations(boolean canCloseAnnotations) {

  }

  @Override
  public void setGutterPopupGroup(@Nullable ActionGroup group) {

  }

  @Override
  public void setPaintBackground(boolean value) {

  }

  @Override
  public void setForceShowLeftFreePaintersArea(boolean value) {

  }

  @Override
  public void setForceShowRightFreePaintersArea(boolean value) {

  }

  @Override
  public void setInitialIconAreaWidth(int width) {

  }

  @Override
  public void registerTextAnnotation(@Nonnull TextAnnotationGutterProvider provider) {

  }

  @Override
  public void registerTextAnnotation(@Nonnull TextAnnotationGutterProvider provider, @Nonnull EditorGutterAction action) {

  }

  @Override
  public boolean isAnnotationsShown() {
    return false;
  }

  @Nonnull
  @Override
  public List<TextAnnotationGutterProvider> getTextAnnotations() {
    return null;
  }

  @Override
  public void closeAllAnnotations() {

  }

  @Override
  public void closeTextAnnotations(@Nonnull Collection<? extends TextAnnotationGutterProvider> annotations) {

  }
}
