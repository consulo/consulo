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
package consulo.desktop.awt.internal.diff.simple;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.fragment.LineFragment;
import consulo.diff.impl.internal.util.DiffGutterRenderer;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.util.Side;
import consulo.diff.util.TextDiffType;
import consulo.document.Document;
import consulo.externalService.statistic.UsageTrigger;
import consulo.ide.impl.diff.DiffDrawUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SimpleDiffChange {
  @Nonnull
  private final SimpleDiffViewer myViewer;

  @Nonnull
  private final LineFragment myFragment;
  @Nullable private final List<DiffFragment> myInnerFragments;

  @Nonnull
  private final List<RangeHighlighter> myHighlighters = new ArrayList<>();
  @Nonnull
  private final List<MyGutterOperation> myOperations = new ArrayList<>();

  private boolean myIsValid = true;
  private int[] myLineStartShifts = new int[2];
  private int[] myLineEndShifts = new int[2];

  public SimpleDiffChange(
    @Nonnull SimpleDiffViewer viewer,
    @Nonnull LineFragment fragment,
    @Nullable LineFragment previousFragment
  ) {
    myViewer = viewer;

    myFragment = fragment;
    myInnerFragments = fragment.getInnerFragments();

    installHighlighter(previousFragment);
  }

  public void installHighlighter(@Nullable LineFragment previousFragment) {
    assert myHighlighters.isEmpty();

    if (myInnerFragments != null) {
      doInstallHighlighterWithInner();
    }
    else {
      doInstallHighlighterSimple();
    }
    doInstallNonSquashedChangesSeparator(previousFragment);

    doInstallActionHighlighters();
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

  private void doInstallHighlighterSimple() {
    createHighlighter(Side.LEFT, false);
    createHighlighter(Side.RIGHT, false);
  }

  private void doInstallHighlighterWithInner() {
    assert myInnerFragments != null;

    createHighlighter(Side.LEFT, true);
    createHighlighter(Side.RIGHT, true);

    for (DiffFragment fragment : myInnerFragments) {
      createInlineHighlighter(fragment, Side.LEFT);
      createInlineHighlighter(fragment, Side.RIGHT);
    }
  }

  private void doInstallNonSquashedChangesSeparator(@Nullable LineFragment previousFragment) {
    createNonSquashedChangesSeparator(previousFragment, Side.LEFT);
    createNonSquashedChangesSeparator(previousFragment, Side.RIGHT);
  }

  private void doInstallActionHighlighters() {
    myOperations.add(createOperation(Side.LEFT));
    myOperations.add(createOperation(Side.RIGHT));
  }

  private void createHighlighter(@Nonnull Side side, boolean ignored) {
    Editor editor = myViewer.getEditor(side);

    TextDiffType type = DiffImplUtil.getLineDiffType(myFragment);
    int startLine = side.getStartLine(myFragment);
    int endLine = side.getEndLine(myFragment);

    myHighlighters.addAll(DiffDrawUtil.createHighlighter(editor, startLine, endLine, type, ignored));
  }

  private void createInlineHighlighter(@Nonnull DiffFragment fragment, @Nonnull Side side) {
    int start = side.getStartOffset(fragment);
    int end = side.getEndOffset(fragment);
    TextDiffType type = DiffImplUtil.getDiffType(fragment);

    int startOffset = side.getStartOffset(myFragment);
    start += startOffset;
    end += startOffset;

    Editor editor = myViewer.getEditor(side);
    myHighlighters.addAll(DiffDrawUtil.createInlineHighlighter(editor, start, end, type));
  }

  private void createNonSquashedChangesSeparator(@Nullable LineFragment previousFragment, @Nonnull Side side) {
    if (previousFragment == null) return;

    int startLine = side.getStartLine(myFragment);
    int endLine = side.getEndLine(myFragment);

    int prevStartLine = side.getStartLine(previousFragment);
    int prevEndLine = side.getEndLine(previousFragment);

    if (startLine == endLine) return;
    if (prevStartLine == prevEndLine) return;
    if (prevEndLine != startLine) return;

    myHighlighters.addAll(DiffDrawUtil.createLineMarker(myViewer.getEditor(side), startLine, TextDiffType.MODIFIED));
  }

  public void updateGutterActions(boolean force) {
    for (MyGutterOperation operation : myOperations) {
      operation.update(force);
    }
  }

  //
  // Getters
  //

  public int getStartLine(@Nonnull Side side) {
    return side.getStartLine(myFragment) + side.select(myLineStartShifts);
  }

  public int getEndLine(@Nonnull Side side) {
    return side.getEndLine(myFragment) + side.select(myLineEndShifts);
  }

  @Nonnull
  public TextDiffType getDiffType() {
    return DiffImplUtil.getLineDiffType(myFragment);
  }

  public boolean isValid() {
    return myIsValid;
  }

  //
  // Shift
  //

  public boolean processChange(int oldLine1, int oldLine2, int shift, @Nonnull Side side) {
    int line1 = getStartLine(side);
    int line2 = getEndLine(side);
    int sideIndex = side.getIndex();

    DiffImplUtil.UpdatedLineRange newRange = DiffImplUtil.updateRangeOnModification(line1, line2, oldLine1, oldLine2, shift);
    myLineStartShifts[sideIndex] += newRange.startLine - line1;
    myLineEndShifts[sideIndex] += newRange.endLine - line2;

    if (newRange.damaged) {
      for (MyGutterOperation operation : myOperations) {
        operation.dispose();
      }
      myOperations.clear();

      myIsValid = false;
    }

    return newRange.damaged;
  }

  //
  // Change applying
  //

  public boolean isSelectedByLine(int line, @Nonnull Side side) {
    int line1 = getStartLine(side);
    int line2 = getEndLine(side);

    return DiffImplUtil.isSelectedByLine(line, line1, line2);
  }

  //
  // Helpers
  //

  @Nonnull
  private MyGutterOperation createOperation(@Nonnull Side side) {
    int offset = side.getStartOffset(myFragment);
    EditorEx editor = myViewer.getEditor(side);
    RangeHighlighter highlighter = editor.getMarkupModel()
      .addRangeHighlighter(offset, offset, HighlighterLayer.ADDITIONAL_SYNTAX, null, HighlighterTargetArea.LINES_IN_RANGE);
    return new MyGutterOperation(side, highlighter);
  }

  private class MyGutterOperation {
    @Nonnull
    private final Side mySide;
    @Nonnull
    private final RangeHighlighter myHighlighter;

    private boolean myCtrlPressed;

    private MyGutterOperation(@Nonnull Side side, @Nonnull RangeHighlighter highlighter) {
      mySide = side;
      myHighlighter = highlighter;

      update(true);
    }

    public void dispose() {
      myHighlighter.dispose();
    }

    public void update(boolean force) {
      if (!force && !areModifiersChanged()) {
        return;
      }
      if (myHighlighter.isValid()) myHighlighter.setGutterIconRenderer(createRenderer());
    }

    private boolean areModifiersChanged() {
      return myCtrlPressed != myViewer.getModifierProvider().isCtrlPressed();
    }

    @Nullable
    public GutterIconRenderer createRenderer() {
      myCtrlPressed = myViewer.getModifierProvider().isCtrlPressed();

      boolean isOtherEditable = DiffImplUtil.isEditable(myViewer.getEditor(mySide.other()));
      boolean isAppendable = myFragment.getStartLine1() != myFragment.getEndLine1() &&
                             myFragment.getStartLine2() != myFragment.getEndLine2();

      if (isOtherEditable) {
        if (myCtrlPressed && isAppendable) {
          return createAppendRenderer(mySide);
        }
        else {
          return createApplyRenderer(mySide);
        }
      }
      return null;
    }
  }

  @Nullable
  private GutterIconRenderer createApplyRenderer(@Nonnull final Side side) {
    return createIconRenderer(side, "Accept", DiffImplUtil.getArrowIcon(side), () -> myViewer.replaceChange(this, side));
  }

  @Nullable
  private GutterIconRenderer createAppendRenderer(@Nonnull final Side side) {
    return createIconRenderer(side, "Append", DiffImplUtil.getArrowDownIcon(side), () -> {
      UsageTrigger.trigger("diff.SimpleDiffChange.Append");
      myViewer.appendChange(this, side);
    });
  }

  @Nullable
  private GutterIconRenderer createIconRenderer(
    @Nonnull final Side sourceSide,
    @Nonnull final String tooltipText,
    @Nonnull final Image icon,
    @Nonnull final Runnable perform
  ) {
    if (!DiffImplUtil.isEditable(myViewer.getEditor(sourceSide.other()))) return null;
    return new DiffGutterRenderer(icon, tooltipText) {
      @Override
      @RequiredUIAccess
      protected void performAction(AnActionEvent e) {
        if (!myIsValid) return;
        final Project project = e.getData(Project.KEY);
        final Document document = myViewer.getEditor(sourceSide.other()).getDocument();
        DiffImplUtil.executeWriteCommand(document, project, "Replace change", perform);
      }
    };
  }
}
