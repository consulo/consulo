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
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.internal.CaretPixelLocationProvider;
import consulo.codeEditor.internal.CaretPixelLocationProvider.CaretPixelLocation;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.internal.DiffInternal;
import consulo.diff.internal.DiffLanguageUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HasSize;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.layout.DockLayout;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static consulo.diff.internal.DiffImplUtil.getDiffType;

/**
 * @author VISTALL
 * @since 2026-08-11
 */
public class UnifiedLineStatusMarkerPopup extends LineStatusMarkerPopupBase {
    public UnifiedLineStatusMarkerPopup(LineStatusTracker tracker, Editor editor, VcsRange range) {
        super(tracker, editor, range);
    }

    @Override
    @RequiredUIAccess
    public void showHintAt(@Nullable InputDetails details) {
        if (!myTracker.isValid()) {
            return;
        }

        Disposable disposable = Disposable.newDisposable();

        List<DiffFragment> wordDiff = computeWordDiff();
        installMasterEditorHighlighters(wordDiff, disposable);

        ActionGroup group = buildActionGroup(details, disposable);

        ActionToolbar toolbar =
            ActionManager.getInstance().createActionToolbar(ActionPlaces.FILEHISTORY_VIEW_TOOLBAR, group, true);
        toolbar.setTargetUIComponent(myEditor.getUIComponent());

        Component preview = createPreview(wordDiff, disposable);

        toolbar.updateActionsAsync().whenCompleteAsync(
            (actions, throwable) -> show(toolbar, preview, details, disposable),
            UIAccess.current()
        );
    }

    @RequiredUIAccess
    private void show(
        ActionToolbar toolbar,
        @Nullable Component preview,
        @Nullable InputDetails details,
        Disposable disposable
    ) {
        LightPopup popup = LightPopup.create(PopupOptions.builder().disableRequestFocus().build());

        DockLayout content = DockLayout.create();
        content.top(toolbar.getUIComponent());
        if (preview != null) {
            content.bottom(preview);
        }

        popup.setContent(content);
        popup.addCloseListener(event -> Disposer.dispose(disposable));

        CaretPixelLocation caretLocation = caretLocation();

        int anchorHeight = caretLocation != null ? caretLocation.height() : myEditor.getLineHeight();

        int x = details != null ? details.getX() : caretLocation != null ? caretLocation.textX() : 0;
        int y = details != null ? details.getY() : caretLocation != null ? caretLocation.y() : 0;

        popup.showAt(myEditor.getUIComponent(), x, y, anchorHeight);
    }

    @RequiredUIAccess
    private @Nullable Component createPreview(@Nullable List<DiffFragment> wordDiff, Disposable disposable) {
        if (myRange.getType() == VcsRange.INSERTED) {
            return null;
        }

        Project project = myTracker.getProject();

        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument(myTracker.getVcsContent(myRange));

        EditorEx viewer = (EditorEx)editorFactory.createViewer(document, project, EditorKind.PREVIEW);
        Disposer.register(disposable, () -> editorFactory.releaseEditor(viewer));

        viewer.setColorsScheme(myEditor.getColorsScheme());
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

        DiffLanguageUtil.setEditorCodeStyle(project, viewer, getFileType());

        viewer.setHighlighter(EditorHighlighterFactory.getInstance()
            .createEditorHighlighter(project, getFileName(myTracker.getDocument())));

        if (wordDiff != null) {
            DiffInternal diffInternal = Application.get().getInstance(DiffInternal.class);

            for (DiffFragment fragment : wordDiff) {
                diffInternal.createInlineHighlighter(
                    viewer,
                    fragment.getStartOffset1(),
                    fragment.getEndOffset1(),
                    getDiffType(fragment)
                );
            }
        }

        Component component = viewer.getUIComponent();
        if (component instanceof HasSize hasSize) {
            hasSize.setWidth(previewWidth(document));
        }
        return component;
    }

    private int previewWidth(Document document) {
        int columns = 0;
        for (int line = 0; line < document.getLineCount(); line++) {
            columns = Math.max(columns, document.getLineEndOffset(line) - document.getLineStartOffset(line));
        }

        int width = myEditor.logicalPositionToXY(new LogicalPosition(0, columns + 1)).x;
        int visibleWidth = myEditor.getScrollingModel().getVisibleArea().width;
        return visibleWidth > 0 ? Math.min(width, visibleWidth) : width;
    }

    private static String getFileName(Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return file == null ? "" : file.getName();
    }

    private @Nullable CaretPixelLocation caretLocation() {
        return myEditor instanceof CaretPixelLocationProvider provider ? provider.getCaretPixelLocation() : null;
    }
}
