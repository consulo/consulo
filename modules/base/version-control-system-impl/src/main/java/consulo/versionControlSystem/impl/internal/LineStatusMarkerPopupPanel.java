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
package consulo.versionControlSystem.impl.internal;

import consulo.application.Application;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.diff.comparison.ByWord;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.internal.DiffInternal;
import consulo.diff.internal.DiffLanguageUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HasSize;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.MouseInputDetails;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.layout.DockLayout;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static consulo.diff.internal.DiffImplUtil.getDiffType;

/**
 * The popup of a changed-lines marker: the actions of the change over a viewer of the text the change replaced.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class LineStatusMarkerPopupPanel {
    private static final Key<LightPopup> ACTIVE_POPUP = Key.create("LineStatusMarkerPopupPanel.activePopup");

    private static final int PREVIEW_INSET = 4;

    private LineStatusMarkerPopupPanel() {
    }

    @RequiredUIAccess
    public static void show(Editor editor,
                            ActionToolbar toolbar,
                            @Nullable Component preview,
                            @Nullable InputDetails details,
                            Disposable disposable) {
        LightPopup previous = editor.getUserData(ACTIVE_POPUP);
        if (previous != null) {
            previous.close();
        }

        LightPopup popup = LightPopup.create(PopupOptions.builder().disableRequestFocus().build());

        DockLayout content = DockLayout.create();
        content.top(toolbar.getUIComponent());
        if (preview != null) {
            content.bottom(preview);
        }

        popup.setContent(content);
        popup.addCloseListener(event -> {
            if (editor.getUserData(ACTIVE_POPUP) == popup) {
                editor.putUserData(ACTIVE_POPUP, null);
            }
            Disposer.dispose(disposable);
        });
        editor.putUserData(ACTIVE_POPUP, popup);

        int lineHeight = editor.getLineHeight();
        int y = editor.logicalPositionToXY(new LogicalPosition(editor.getCaretModel().getLogicalPosition().line, 0)).y;
        if (details instanceof MouseInputDetails && lineHeight > 0) {
            int mouseY = details.getY();
            int delta = (y - mouseY) % lineHeight;
            if (delta <= 0) {
                delta += lineHeight;
            }
            y = mouseY + delta - lineHeight;
        }

        popup.showAt(editor.getContentUIComponent(), 0, y, lineHeight);
    }

    @RequiredUIAccess
    public static Component createPreview(@Nullable Project project,
                                          Editor mainEditor,
                                          CharSequence vcsContent,
                                          FileType fileType,
                                          String fileName,
                                          @Nullable List<DiffFragment> wordDiff,
                                          Disposable disposable) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument(vcsContent);

        EditorEx viewer = (EditorEx) editorFactory.createViewer(document, project, EditorKind.PREVIEW);
        Disposer.register(disposable, () -> editorFactory.releaseEditor(viewer));

        viewer.setColorsScheme(mainEditor.getColorsScheme());
        viewer.setCaretVisible(false);

        EditorSettings settings = viewer.getSettings();
        settings.setLineNumbersShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setGutterIconsShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setCaretRowShown(false);
        settings.setRightMarginShown(false);
        settings.setAdditionalLinesCount(0);
        settings.setAdditionalColumnsCount(0);

        if (viewer.getMarkupModel() instanceof EditorMarkupModel markupModel) {
            markupModel.setErrorStripeVisible(false);
        }

        DiffLanguageUtil.setEditorCodeStyle(project, viewer, fileType);

        viewer.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileName));

        if (wordDiff != null) {
            DiffInternal diffInternal = Application.get().getInstance(DiffInternal.class);

            for (DiffFragment fragment : wordDiff) {
                diffInternal.createInlineHighlighter(viewer, fragment.getStartOffset1(), fragment.getEndOffset1(), getDiffType(fragment));
            }
        }

        Component component = viewer.getUIComponent();
        if (component instanceof HasSize hasSize) {
            fitToContent(viewer, document, mainEditor, hasSize);
        }
        return component;
    }

    @RequiredUIAccess
    public static @Nullable List<DiffFragment> computeWordDiff(CharSequence vcsContent, CharSequence currentContent) {
        return BackgroundTaskUtil.tryComputeFast(
            indicator -> ByWord.compare(vcsContent, currentContent, ComparisonPolicy.DEFAULT, indicator),
            Registry.intValue("diff.status.tracker.byword.delay")
        );
    }

    public static void installMasterEditorWordHighlighters(Editor editor,
                                                           int currentStartOffset,
                                                           List<DiffFragment> wordDiff,
                                                           Disposable parentDisposable) {
        List<RangeHighlighter> highlighters = new ArrayList<>();

        DiffInternal diffInternal = Application.get().getInstance(DiffInternal.class);
        for (DiffFragment fragment : wordDiff) {
            int currentStart = currentStartOffset + fragment.getStartOffset2();
            int currentEnd = currentStartOffset + fragment.getEndOffset2();
            highlighters.addAll(diffInternal.createInlineHighlighter(editor, currentStart, currentEnd, getDiffType(fragment)));
        }

        Disposer.register(parentDisposable, () -> {
            for (RangeHighlighter highlighter : highlighters) {
                highlighter.dispose();
            }
        });
    }

    @RequiredUIAccess
    private static void fitToContent(EditorEx viewer, Document document, Editor mainEditor, HasSize hasSize) {
        int lastLine = Math.max(0, document.getLineCount() - 1);

        int width = 0;
        for (int line = 0; line <= lastLine; line++) {
            LogicalPosition end = viewer.offsetToLogicalPosition(document.getLineEndOffset(line));
            width = Math.max(width, viewer.logicalPositionToXY(new LogicalPosition(line, end.column + 1)).x);
        }
        width += PREVIEW_INSET;

        int height = viewer.logicalPositionToXY(new LogicalPosition(lastLine, 0)).y + viewer.getLineHeight() + PREVIEW_INSET;

        int visibleWidth = mainEditor.getScrollingModel().getVisibleArea().width;
        boolean clipped = visibleWidth > 0 && width > visibleWidth;

        viewer.setVerticalScrollbarVisible(false);
        viewer.setHorizontalScrollbarVisible(clipped);

        hasSize.setWidth(clipped ? visibleWidth : width);
        hasSize.setHeight(clipped ? height + viewer.getLineHeight() : height);
    }
}
