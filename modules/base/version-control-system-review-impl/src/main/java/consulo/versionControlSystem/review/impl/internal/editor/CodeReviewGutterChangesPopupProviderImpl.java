// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.review.impl.internal.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.diff.fragment.DiffFragment;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.editor.CodeReviewChangedRange;
import consulo.review.editor.CodeReviewEditorGutterActionableChangesModel;
import consulo.review.impl.internal.editor.CodeReviewGutterChangesPopupProvider;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.impl.internal.LineStatusMarkerPopupPanel;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class CodeReviewGutterChangesPopupProviderImpl implements CodeReviewGutterChangesPopupProvider {
    private static final String SHOW_PREV_CHANGE_MARKER = "VcsShowPrevChangeMarker";
    private static final String SHOW_NEXT_CHANGE_MARKER = "VcsShowNextChangeMarker";

    @RequiredUIAccess
    @Override
    public void showPopup(Editor editor,
                          CodeReviewEditorGutterActionableChangesModel model,
                          CodeReviewChangedRange range,
                          @Nullable InputDetails details) {
        Disposable disposable = Disposable.newDisposable("Code review changes popup");
        Document document = editor.getDocument();

        String baseContent = range.baseLine1() == range.baseLine2() ? null : model.getBaseContent(range.baseLine1(), range.baseLine2());
        String vcsContent = baseContent == null ? null : StringUtil.trimEnd(baseContent, "\n");

        List<DiffFragment> wordDiff = null;
        if (vcsContent != null && model.getShouldHighlightDiffRanges()) {
            CharSequence currentContent = getLinesContent(document, range.line1(), range.line2());
            if (currentContent.length() > 0) {
                wordDiff = LineStatusMarkerPopupPanel.computeWordDiff(vcsContent, currentContent);
                if (wordDiff != null) {
                    LineStatusMarkerPopupPanel.installMasterEditorWordHighlighters(editor,
                        document.getLineStartOffset(range.line1()),
                        wordDiff,
                        disposable);
                }
            }
        }

        Component preview = null;
        if (vcsContent != null) {
            VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            FileType fileType = file == null ? PlainTextFileType.INSTANCE : file.getFileType();
            preview = LineStatusMarkerPopupPanel.createPreview(editor.getProject(),
                editor,
                vcsContent,
                fileType,
                file == null ? "" : file.getName(),
                wordDiff,
                disposable);
        }

        DefaultActionGroup group = new DefaultActionGroup();
        for (AnAction action : createActions(editor, model, range, details)) {
            group.add(action);
        }

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.FILEHISTORY_VIEW_TOOLBAR, group, true);
        toolbar.setTargetUIComponent(editor.getContentUIComponent());

        Component popupPreview = preview;
        toolbar.updateActionsAsync().whenCompleteAsync(
            (actions, throwable) -> LineStatusMarkerPopupPanel.show(editor, toolbar, popupPreview, details, disposable),
            UIAccess.current()
        );
    }

    private List<AnAction> createActions(Editor editor,
                                                              CodeReviewEditorGutterActionableChangesModel model,
                                                              CodeReviewChangedRange range,
                                                              @Nullable InputDetails details) {
        List<AnAction> actions = new ArrayList<>();
        actions.add(new ShowChangeMarkerAction(SHOW_PREV_CHANGE_MARKER, editor, model, range, true));
        actions.add(new ShowChangeMarkerAction(SHOW_NEXT_CHANGE_MARKER, editor, model, range, false));
        actions.add(new CopyLineStatusRangeAction(model, range));
        actions.add(new ShowDiffAction(model, range));
        actions.add(new ToggleByWordDiffAction(editor, model, range, details));
        return actions;
    }

    private static CharSequence getLinesContent(Document document, int line1, int line2) {
        if (line1 >= line2) {
            return "";
        }
        int start = document.getLineStartOffset(line1);
        int end = document.getLineEndOffset(line2 - 1);
        return document.getImmutableCharSequence().subSequence(start, end);
    }

    private static @Nullable CodeReviewChangedRange getNextRange(List<CodeReviewChangedRange> ranges, int line) {
        boolean found = false;
        for (CodeReviewChangedRange range : ranges) {
            if (isSelectedByLine(line, range.line1(), range.line2())) {
                found = true;
            }
            else if (found) {
                return range;
            }
        }
        return null;
    }

    private static boolean isSelectedByLine(int line, int line1, int line2) {
        if (line1 == line2 && line == line1) {
            return true;
        }
        return line >= line1 && line < line2;
    }

    @RequiredUIAccess
    private void scrollAndShow(Editor editor, CodeReviewEditorGutterActionableChangesModel model, CodeReviewChangedRange range) {
        Document document = editor.getDocument();
        int line = Math.min(range.line1() == range.line2() ? range.line2() : range.line2() - 1, Math.max(document.getLineCount(), 1) - 1);
        editor.getCaretModel().moveToOffset(document.getLineStartOffset(line));
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        editor.getScrollingModel().runActionOnScrollingFinished(() -> showPopup(editor, model, range, null));
    }

    private class ShowChangeMarkerAction extends DumbAwareAction {
        private final Editor myEditor;
        private final CodeReviewEditorGutterActionableChangesModel myModel;
        private final CodeReviewChangedRange myRange;
        private final boolean myPrevious;

        ShowChangeMarkerAction(String id,
                               Editor editor,
                               CodeReviewEditorGutterActionableChangesModel model,
                               CodeReviewChangedRange range,
                               boolean previous) {
            myEditor = editor;
            myModel = model;
            myRange = range;
            myPrevious = previous;
            ActionUtil.copyFrom(this, id);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<CodeReviewChangedRange> ranges = myModel.getReviewRanges().get();
            if (ranges == null) {
                return;
            }
            List<CodeReviewChangedRange> ordered = new ArrayList<>(ranges);
            if (myPrevious) {
                Collections.reverse(ordered);
            }
            CodeReviewChangedRange targetRange = getNextRange(ordered, myRange.line1());
            if (targetRange != null) {
                scrollAndShow(myEditor, myModel, targetRange);
            }
        }
    }

    private static class CopyLineStatusRangeAction extends DumbAwareAction {
        private final CodeReviewEditorGutterActionableChangesModel myModel;
        private final CodeReviewChangedRange myRange;

        CopyLineStatusRangeAction(CodeReviewEditorGutterActionableChangesModel model, CodeReviewChangedRange range) {
            myModel = model;
            myRange = range;
            ActionUtil.copyFrom(this, IdeActions.ACTION_COPY);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            if (myRange.baseLine1() == myRange.baseLine2()) {
                return;
            }
            String content = myModel.getBaseContent(myRange.baseLine1(), myRange.baseLine2());
            if (content != null) {
                UIAccess.current().getClipboard().setText(content);
            }
        }
    }

    private static class ShowDiffAction extends DumbAwareAction {
        private final CodeReviewEditorGutterActionableChangesModel myModel;
        private final CodeReviewChangedRange myRange;

        ShowDiffAction(CodeReviewEditorGutterActionableChangesModel model, CodeReviewChangedRange range) {
            super(ReviewLocalize.reviewDiffActionShowText(), ReviewLocalize.reviewDiffActionShowDescription(), PlatformIconGroup.actionsDiff());
            myModel = model;
            myRange = range;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            myModel.showDiff(myRange.line1());
        }
    }

    private class ToggleByWordDiffAction extends ToggleAction {
        private final Editor myEditor;
        private final CodeReviewEditorGutterActionableChangesModel myModel;
        private final CodeReviewChangedRange myRange;
        private final @Nullable InputDetails myDetails;

        ToggleByWordDiffAction(Editor editor,
                               CodeReviewEditorGutterActionableChangesModel model,
                               CodeReviewChangedRange range,
                               @Nullable InputDetails details) {
            super(ReviewLocalize.reviewEditorActionHighlightLinesText(), LocalizeValue.empty(), PlatformIconGroup.generalHighlighting());
            myEditor = editor;
            myModel = model;
            myRange = range;
            myDetails = details;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myModel.getShouldHighlightDiffRanges();
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            myModel.setShouldHighlightDiffRanges(state);
            showPopup(myEditor, myModel, myRange, myDetails);
        }
    }
}
