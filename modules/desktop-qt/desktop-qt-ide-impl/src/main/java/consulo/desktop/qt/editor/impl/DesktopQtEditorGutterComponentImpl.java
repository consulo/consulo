/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.editor.impl;

import consulo.codeEditor.EditorGutterAction;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.LineNumberConverter;
import consulo.codeEditor.TextAnnotationGutterProvider;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.ui.ex.action.ActionGroup;
import org.jspecify.annotations.Nullable;

import java.awt.Point;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorGutterComponentImpl implements EditorGutterComponentEx {
    private final DesktopQtEditorImpl myEditor;

    public DesktopQtEditorGutterComponentImpl(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    private @Nullable DesktopQtEditorGutterWidget getWidget() {
        DesktopQtEditorWidget surface = myEditor.getSurface();
        return surface == null ? null : surface.getGutter();
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
        DesktopQtEditorGutterWidget widget = getWidget();
        return widget == null ? 0 : widget.separatorOffset();
    }

    @Override
    public void revalidateMarkup() {
        DesktopQtEditorGutterWidget widget = getWidget();
        if (widget != null) {
            widget.update();
        }
    }

    /**
     * Line markers would start where the numbers end, and nothing draws them yet - so the whole gutter is the
     * number area and the marker area is empty at its right edge.
     */
    @Override
    public int getLineMarkerAreaOffset() {
        return getWhitespaceSeparatorOffset();
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
    }

    @Override
    public void registerTextAnnotation(TextAnnotationGutterProvider provider, EditorGutterAction action) {
    }

    @Override
    public boolean isAnnotationsShown() {
        return false;
    }

    @Override
    public List<TextAnnotationGutterProvider> getTextAnnotations() {
        return null;
    }

    @Override
    public void closeAllAnnotations() {
    }

    @Override
    public void closeTextAnnotations(Collection<? extends TextAnnotationGutterProvider> annotations) {
    }
}
