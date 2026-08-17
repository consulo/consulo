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
package consulo.desktop.qt.editor.impl.internal;

import consulo.codeEditor.EditorGutterAction;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.LineNumberConverter;
import consulo.codeEditor.TextAnnotationGutterProvider;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.ui.ex.action.ActionGroup;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Point;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorGutterComponentImpl implements EditorGutterComponentEx {
    private final DesktopQtEditorImpl myEditor;

    /**
     * The platform hangs the state of the gutter off client properties of a swing component - the breakpoint
     * promoter puts the icon it wants drawn on the hovered line there, and the awt gutter is itself that
     * component. Nothing here is ever shown, so it stands in purely as the bag those properties live in.
     */
    private final JComponent myProperties = new JComponent() {
    };

    private @Nullable ActionGroup myGutterPopupGroup;

    private boolean myShowDefaultGutterPopup = true;

    private @Nullable Map<Integer, List<GutterMark>> myRenderersByLine;

    public DesktopQtEditorGutterComponentImpl(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    @Override
    public JComponent getComponent() {
        return myProperties;
    }

    @Override
    public void repaint() {
        dropRenderersCache();

        DesktopQtEditorGutterWidget widget = getWidget();
        if (widget != null) {
            widget.update();
        }
    }

    private @Nullable DesktopQtEditorGutterWidget getWidget() {
        DesktopQtEditorWidget surface = myEditor.getSurface();
        return surface == null ? null : surface.getGutter();
    }

    @Override
    public @Nullable FoldRegion findFoldingAnchorAt(int x, int y) {
        return null;
    }

    /**
     * Everything the markup wants an icon drawn for on a row - a breakpoint above all. Both models carry them:
     * the editor's own, and the document's, which is where the debugger puts its marks.
     *
     * @param line a visual line, as the awt gutter keys its cache by
     */
    @Override
    public List<GutterMark> getGutterRenderers(int line) {
        return renderersByLine().getOrDefault(line, List.of());
    }

    /**
     * Every mark in the document keyed by the row it stands on, built in one pass over the markup and kept until
     * the markup changes. Asking each row separately would walk every highlighter again for every row, and the
     * width of the icon column is measured over all of them on every layout.
     */
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

    /**
     * Drops what was read off the markup. Anything moving a mark - the markup changing, or folding moving the row
     * it is shown on - has to come through here or the icons stay where they were.
     */
    public void dropRenderersCache() {
        myRenderersByLine = null;
    }

    @Override
    public int getWhitespaceSeparatorOffset() {
        DesktopQtEditorGutterWidget widget = getWidget();
        return widget == null ? 0 : widget.separatorOffset();
    }

    @Override
    public void revalidateMarkup() {
        dropRenderersCache();

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
