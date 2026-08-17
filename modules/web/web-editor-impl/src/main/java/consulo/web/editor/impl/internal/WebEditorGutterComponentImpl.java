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
package consulo.web.editor.impl.internal;

import consulo.codeEditor.*;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.ui.ex.action.ActionGroup;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 07/08/2021
 */
public class WebEditorGutterComponentImpl implements EditorGutterComponentEx {
  private final WebEditorImpl myEditor;

  private final JComponent myPropertyHolder = new JPanel();

  private final List<TextAnnotationGutterProvider> myTextAnnotationGutters = new ArrayList<>();
  private final Map<TextAnnotationGutterProvider, EditorGutterAction> myProviderToListener = new HashMap<>();

  public WebEditorGutterComponentImpl(WebEditorImpl editor) {
    myEditor = editor;
  }

  public @Nullable EditorGutterAction getTextAnnotationAction(TextAnnotationGutterProvider provider) {
    return myProviderToListener.get(provider);
  }

  @Override
  public JComponent getComponent() {
    return myPropertyHolder;
  }

  /**
   * The line status tracker asks every gutter of its document to repaint whenever its ranges change, and that
   * is the only signal the tracker gives before an editor has looked it up.
   */
  @Override
  public void repaint() {
    myEditor.scheduleGutterBandsUpdate();

    myEditor.scheduleGutterHoverUpdate();
  }

  @Override
  public @Nullable FoldRegion findFoldingAnchorAt(int x, int y) {
    return null;
  }

  
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
    myEditor.scheduleTextAnnotationsUpdate();
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

  @Override
  public @Nullable Point getCenterPoint(GutterIconRenderer renderer) {
    return null;
  }

  @Override
  public void setLineNumberConverter(LineNumberConverter primaryConverter, @Nullable LineNumberConverter additionalConverter) {

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
  public void registerTextAnnotation(TextAnnotationGutterProvider provider) {
    myTextAnnotationGutters.add(provider);

    myEditor.scheduleTextAnnotationsUpdate();
  }

  @Override
  public void registerTextAnnotation(TextAnnotationGutterProvider provider, EditorGutterAction action) {
    myProviderToListener.put(provider, action);

    registerTextAnnotation(provider);
  }

  @Override
  public boolean isAnnotationsShown() {
    return !myTextAnnotationGutters.isEmpty();
  }


  @Override
  public List<TextAnnotationGutterProvider> getTextAnnotations() {
    return List.copyOf(myTextAnnotationGutters);
  }

  @Override
  public void closeAllAnnotations() {
    closeTextAnnotations(List.copyOf(myTextAnnotationGutters));
  }

  @Override
  public void closeTextAnnotations(Collection<? extends TextAnnotationGutterProvider> annotations) {
    Set<TextAnnotationGutterProvider> closed = new HashSet<>();

    for (TextAnnotationGutterProvider provider : annotations) {
      if (myTextAnnotationGutters.remove(provider)) {
        myProviderToListener.remove(provider);

        closed.add(provider);
      }
    }

    for (TextAnnotationGutterProvider provider : closed) {
      provider.gutterClosed();
    }

    myEditor.scheduleTextAnnotationsUpdate();
  }
}
