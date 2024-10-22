/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff.fragment;

import consulo.application.AllIcons;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.diff.fragment.LineFragment;
import consulo.diff.impl.internal.fragment.ChangedBlock;
import consulo.diff.impl.internal.util.DiffGutterRenderer;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.impl.internal.util.DiffImplUtil.UpdatedLineRange;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.util.LineRange;
import consulo.diff.util.Side;
import consulo.document.Document;
import consulo.ide.impl.diff.DiffDrawUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UnifiedDiffChange {
    @Nonnull
    private final UnifiedDiffViewer myViewer;
    @Nonnull
    private final EditorEx myEditor;

    // Boundaries of this change in myEditor. If current state is out-of-date - approximate value.
    private int myLine1;
    private int myLine2;

    @Nonnull
    private final LineFragment myLineFragment;

    @Nonnull
    private final List<RangeHighlighter> myHighlighters = new ArrayList<>();
    @Nonnull
    private final List<MyGutterOperation> myOperations = new ArrayList<>();

    @RequiredUIAccess
    public UnifiedDiffChange(@Nonnull UnifiedDiffViewer viewer, @Nonnull ChangedBlock block) {
        myViewer = viewer;
        myEditor = viewer.getEditor();

        myLine1 = block.getLine1();
        myLine2 = block.getLine2();
        myLineFragment = block.getLineFragment();

        LineRange deleted = block.getRange1();
        LineRange inserted = block.getRange2();

        installHighlighter(deleted, inserted);
    }

    public void destroyHighlighter() {
        for (RangeHighlighter highlighter : myHighlighters) {
            highlighter.dispose();
        }
        myHighlighters.clear();

        for (MyGutterOperation operation : myOperations) {
            operation.dispose();
        }
        myOperations.clear();
    }

    @RequiredUIAccess
    private void installHighlighter(@Nonnull LineRange deleted, @Nonnull LineRange inserted) {
        assert myHighlighters.isEmpty();

        doInstallHighlighters(deleted, inserted);
        doInstallActionHighlighters();
    }

    @RequiredUIAccess
    private void doInstallActionHighlighters() {
        boolean leftEditable = myViewer.isEditable(Side.LEFT, false);
        boolean rightEditable = myViewer.isEditable(Side.RIGHT, false);

        if (leftEditable && rightEditable) {
            myOperations.add(createOperation(Side.LEFT));
            myOperations.add(createOperation(Side.RIGHT));
        }
        else if (rightEditable) {
            myOperations.add(createOperation(Side.LEFT));
        }
    }

    private void doInstallHighlighters(@Nonnull LineRange deleted, @Nonnull LineRange inserted) {
        myHighlighters.addAll(DiffDrawUtil.createUnifiedChunkHighlighters(myEditor, deleted, inserted, myLineFragment.getInnerFragments()));
    }

    public int getLine1() {
        return myLine1;
    }

    public int getLine2() {
        return myLine2;
    }

    /*
     * Warning: It does not updated on document change. Check myViewer.isStateInconsistent() before use.
     */
    @Nonnull
    public LineFragment getLineFragment() {
        return myLineFragment;
    }

    public void processChange(int oldLine1, int oldLine2, int shift) {
        UpdatedLineRange newRange = DiffImplUtil.updateRangeOnModification(myLine1, myLine2, oldLine1, oldLine2, shift);
        myLine1 = newRange.startLine;
        myLine2 = newRange.endLine;
    }

    //
    // Gutter
    //

    @RequiredUIAccess
    public void updateGutterActions() {
        for (MyGutterOperation operation : myOperations) {
            operation.update();
        }
    }

    @Nonnull
    @RequiredUIAccess
    private MyGutterOperation createOperation(@Nonnull Side sourceSide) {
        int offset = myEditor.getDocument().getLineStartOffset(myLine1);
        RangeHighlighter highlighter = myEditor.getMarkupModel()
            .addRangeHighlighter(offset, offset, HighlighterLayer.ADDITIONAL_SYNTAX, null, HighlighterTargetArea.LINES_IN_RANGE);
        return new MyGutterOperation(sourceSide, highlighter);
    }

    private class MyGutterOperation {
        @Nonnull
        private final Side mySide;
        @Nonnull
        private final RangeHighlighter myHighlighter;

        @RequiredUIAccess
        private MyGutterOperation(@Nonnull Side sourceSide, @Nonnull RangeHighlighter highlighter) {
            mySide = sourceSide;
            myHighlighter = highlighter;

            update();
        }

        public void dispose() {
            myHighlighter.dispose();
        }

        @RequiredUIAccess
        public void update() {
            if (myHighlighter.isValid()) {
                myHighlighter.setGutterIconRenderer(createRenderer());
            }
        }

        @Nullable
        @RequiredUIAccess
        public GutterIconRenderer createRenderer() {
            if (myViewer.isStateIsOutOfDate()) {
                return null;
            }
            if (!myViewer.isEditable(mySide.other(), true)) {
                return null;
            }

            if (mySide.isLeft()) {
                return createIconRenderer(mySide, DiffLocalize.actionPresentationDiffRevertText(), AllIcons.Diff.Remove);
            }
            else {
                return createIconRenderer(mySide, DiffLocalize.actionPresentationDiffAcceptText(), AllIcons.Actions.Checked);
            }
        }
    }

    private GutterIconRenderer createIconRenderer(
        @Nonnull final Side sourceSide,
        @Nonnull final LocalizeValue tooltipText,
        @Nonnull final Image icon
    ) {
        return new DiffGutterRenderer(icon, tooltipText) {
            @Override
            @RequiredUIAccess
            protected void performAction(AnActionEvent e) {
                if (myViewer.isStateIsOutOfDate()) {
                    return;
                }
                if (!myViewer.isEditable(sourceSide.other(), true)) {
                    return;
                }

                final Project project = e.getData(Project.KEY);
                final Document document = myViewer.getDocument(sourceSide.other());

                DiffImplUtil.newWriteCommand(() -> {
                        myViewer.replaceChange(UnifiedDiffChange.this, sourceSide);
                        myViewer.scheduleRediff();
                    })
                    .withProject(project)
                    .withDocument(document)
                    .withName(DiffLocalize.messageReplaceChangeCommand())
                    .execute();
                // applyChange() will schedule rediff, but we want to try to do it in sync
                // and we can't do it inside write action
                myViewer.rediff();
            }
        };
    }
}
