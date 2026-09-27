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
package consulo.it.internal.editor;

import consulo.codeEditor.EditorGutterAction;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.LineNumberConverter;
import consulo.codeEditor.TextAnnotationGutterProvider;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.ui.ex.action.ActionGroup;
import org.jspecify.annotations.Nullable;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gutter of a {@link HeadlessEditor}. Nothing is drawn, but the marks the markup asks an icon for are still
 * collected, so that the code which measures or enumerates them works against real data.
 *
 * @author VISTALL
 * @since 2026-09-26
 */
public class HeadlessEditorGutter implements EditorGutterComponentEx {
    private final CodeEditorBase myEditor;

    private final List<TextAnnotationGutterProvider> myTextAnnotations = new ArrayList<>();

    private @Nullable ActionGroup myGutterPopupGroup;

    private boolean myShowDefaultGutterPopup = true;

    private @Nullable Map<Integer, List<GutterMark>> myRenderersByLine;

    public HeadlessEditorGutter(CodeEditorBase editor) {
        myEditor = editor;
    }

    @Override
    public @Nullable FoldRegion findFoldingAnchorAt(int x, int y) {
        return null;
    }

    @Override
    public List<GutterMark> getGutterRenderers(int line) {
        return renderersByLine().getOrDefault(line, List.of());
    }

    private Map<Integer, List<GutterMark>> renderersByLine() {
        Map<Integer, List<GutterMark>> cache = myRenderersByLine;
        if (cache != null) {
            return cache;
        }

        cache = new HashMap<>();

        collectRenderers(myEditor.getFilteredDocumentMarkupModel(), cache);
        collectRenderers(myEditor.getMarkupModel(), cache);

        myRenderersByLine = cache;

        return cache;
    }

    private void collectRenderers(@Nullable MarkupModel model, Map<Integer, List<GutterMark>> into) {
        if (model == null) {
            return;
        }

        for (RangeHighlighter highlighter : model.getAllHighlighters()) {
            GutterMark renderer = highlighter.getGutterIconRenderer();

            if (renderer != null && highlighter.isValid()) {
                into.computeIfAbsent(myEditor.offsetToVisualLine(highlighter.getStartOffset()), line -> new ArrayList<>())
                    .add(renderer);
            }
        }
    }

    public void dropRenderersCache() {
        myRenderersByLine = null;
    }

    @Override
    public int getWhitespaceSeparatorOffset() {
        return 0;
    }

    @Override
    public void revalidateMarkup() {
        dropRenderersCache();
    }

    @Override
    public void repaint() {
        dropRenderersCache();
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
        return new Point(0, 0);
    }

    @Override
    public @Nullable GutterIconRenderer getGutterRenderer(Point p) {
        return null;
    }

    @Override
    public boolean isInsideMarkerArea(MouseEvent e) {
        return false;
    }

    @Override
    public void setLineNumberConverter(LineNumberConverter primaryConverter, @Nullable LineNumberConverter additionalConverter) {
    }

    @Override
    public void setShowDefaultGutterPopup(boolean show) {
        myShowDefaultGutterPopup = show;
    }

    public boolean isShowDefaultGutterPopup() {
        return myShowDefaultGutterPopup;
    }

    @Override
    public void setCanCloseAnnotations(boolean canCloseAnnotations) {
    }

    @Override
    public void setGutterPopupGroup(@Nullable ActionGroup group) {
        myGutterPopupGroup = group;
    }

    public @Nullable ActionGroup getGutterPopupGroup() {
        return myGutterPopupGroup;
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
        myTextAnnotations.add(provider);
    }

    @Override
    public void registerTextAnnotation(TextAnnotationGutterProvider provider, EditorGutterAction action) {
        myTextAnnotations.add(provider);
    }

    @Override
    public boolean isAnnotationsShown() {
        return false;
    }

    @Override
    public List<TextAnnotationGutterProvider> getTextAnnotations() {
        return List.copyOf(myTextAnnotations);
    }

    @Override
    public void closeAllAnnotations() {
        myTextAnnotations.clear();
    }

    @Override
    public void closeTextAnnotations(Collection<? extends TextAnnotationGutterProvider> annotations) {
        myTextAnnotations.removeAll(annotations);
    }
}
