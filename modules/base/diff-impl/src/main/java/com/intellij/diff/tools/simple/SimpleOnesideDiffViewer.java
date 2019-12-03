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
package com.intellij.diff.tools.simple;

import com.intellij.diff.DiffContext;
import com.intellij.diff.actions.AllLinesIterator;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.tools.util.DiffDataKeys;
import com.intellij.diff.tools.util.base.HighlightPolicy;
import com.intellij.diff.tools.util.base.TextDiffViewerUtil;
import com.intellij.diff.tools.util.side.OnesideTextDiffViewer;
import com.intellij.diff.util.DiffDrawUtil;
import com.intellij.diff.util.DiffUtil;
import com.intellij.diff.util.LineRange;
import com.intellij.diff.util.TextDiffType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnSeparator;
import consulo.logging.Logger;
import com.intellij.openapi.diff.DiffNavigationContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.progress.ProgressIndicator;
import consulo.util.dataholder.Key;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.diff.util.DiffUtil.getLineCount;

public class SimpleOnesideDiffViewer extends OnesideTextDiffViewer {
  public static final Logger LOG = Logger.getInstance(SimpleOnesideDiffViewer.class);

  @Nonnull
  private final MyInitialScrollHelper myInitialScrollHelper = new MyInitialScrollHelper();

  @Nonnull
  private final List<RangeHighlighter> myHighlighters = new ArrayList<>();

  public SimpleOnesideDiffViewer(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    super(context, (ContentDiffRequest)request);
  }

  @Override
  @RequiredUIAccess
  protected void onDispose() {
    for (RangeHighlighter highlighter : myHighlighters) {
      highlighter.dispose();
    }
    myHighlighters.clear();
    super.onDispose();
  }

  @Nonnull
  @Override
  protected List<AnAction> createToolbarActions() {
    List<AnAction> group = new ArrayList<>();

    group.add(new MyIgnorePolicySettingAction());
    group.add(new MyHighlightPolicySettingAction());
    group.add(new MyReadOnlyLockAction());
    group.add(myEditorSettingsAction);

    group.add(AnSeparator.getInstance());
    group.addAll(super.createToolbarActions());

    return group;
  }

  @Nonnull
  @Override
  protected List<AnAction> createPopupActions() {
    List<AnAction> group = new ArrayList<>();

    group.add(AnSeparator.getInstance());
    group.add(new MyIgnorePolicySettingAction().getPopupGroup());
    group.add(AnSeparator.getInstance());
    group.add(new MyHighlightPolicySettingAction().getPopupGroup());

    group.add(AnSeparator.getInstance());
    group.addAll(super.createPopupActions());

    return group;
  }

  @Override
  @RequiredUIAccess
  protected void processContextHints() {
    super.processContextHints();
    myInitialScrollHelper.processContext(myRequest);
  }

  @Override
  @RequiredUIAccess
  protected void updateContextHints() {
    super.updateContextHints();
    myInitialScrollHelper.updateContext(myRequest);
  }

  //
  // Diff
  //

  @Override
  @Nonnull
  protected Runnable performRediff(@Nonnull final ProgressIndicator indicator) {
    return () -> {
      clearDiffPresentation();

      boolean shouldHighlight = getTextSettings().getHighlightPolicy() != HighlightPolicy.DO_NOT_HIGHLIGHT;
      if (shouldHighlight) {
        final DocumentContent content = getContent();
        final Document document = content.getDocument();

        TextDiffType type = getSide().select(TextDiffType.DELETED, TextDiffType.INSERTED);

        myHighlighters.addAll(DiffDrawUtil.createHighlighter(getEditor(), 0, getLineCount(document), type, false));
      }

      myInitialScrollHelper.onRediff();
    };
  }


  private void clearDiffPresentation() {
    myPanel.resetNotifications();

    for (RangeHighlighter highlighter : myHighlighters) {
      highlighter.dispose();
    }
    myHighlighters.clear();
  }

  //
  // Impl
  //

  private void doScrollToChange(final boolean animated) {
    DiffUtil.moveCaret(getEditor(), 0);
    DiffUtil.scrollEditor(getEditor(), 0, animated);
  }

  protected boolean doScrollToContext(@Nonnull DiffNavigationContext context) {
    if (getSide().isLeft()) return false;

    AllLinesIterator allLinesIterator = new AllLinesIterator(getEditor().getDocument());
    int line = context.contextMatchCheck(allLinesIterator);
    if (line == -1) return false;

    scrollToLine(line);
    return true;
  }

  //
  // Misc
  //

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static boolean canShowRequest(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    return OnesideTextDiffViewer.canShowRequest(context, request);
  }

  //
  // Actions
  //

  private class MyReadOnlyLockAction extends TextDiffViewerUtil.EditorReadOnlyLockAction {
    public MyReadOnlyLockAction() {
      super(getContext(), getEditableEditors());
    }
  }

  //
  // Modification operations
  //

  private class MyHighlightPolicySettingAction extends TextDiffViewerUtil.HighlightPolicySettingAction {
    public MyHighlightPolicySettingAction() {
      super(getTextSettings());
    }

    @Override
    protected void onSettingsChanged() {
      rediff();
    }
  }

  private class MyIgnorePolicySettingAction extends TextDiffViewerUtil.IgnorePolicySettingAction {
    public MyIgnorePolicySettingAction() {
      super(getTextSettings());
    }

    @Override
    protected void onSettingsChanged() {
      rediff();
    }
  }

  //
  // Helpers
  //

  @javax.annotation.Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (DiffDataKeys.CURRENT_CHANGE_RANGE == dataId) {
      int lineCount = getLineCount(getEditor().getDocument());
      return new LineRange(0, lineCount);
    }
    return super.getData(dataId);
  }

  private class MyInitialScrollHelper extends MyInitialScrollPositionHelper {
    @Override
    protected boolean doScrollToChange() {
      if (myScrollToChange == null) return false;
      SimpleOnesideDiffViewer.this.doScrollToChange(false);
      return true;
    }

    @Override
    protected boolean doScrollToFirstChange() {
      SimpleOnesideDiffViewer.this.doScrollToChange(false);
      return true;
    }

    @Override
    protected boolean doScrollToContext() {
      if (myNavigationContext == null) return false;
      return SimpleOnesideDiffViewer.this.doScrollToContext(myNavigationContext);
    }

    @Override
    protected boolean doScrollToPosition() {
      if (myCaretPosition == null) return false;

      LogicalPosition position = getSide().select(myCaretPosition);
      getEditor().getCaretModel().moveToLogicalPosition(position);

      if (myEditorsPosition != null && myEditorsPosition.isSame(position)) {
        DiffUtil.scrollToPoint(getEditor(), myEditorsPosition.myPoints[0], false);
      }
      else {
        DiffUtil.scrollToCaret(getEditor(), false);
      }
      return true;
    }

    @javax.annotation.Nullable
    @Override
    protected LogicalPosition[] getCaretPositions() {
      int index = getSide().getIndex();
      int otherIndex = getSide().other().getIndex();

      LogicalPosition[] carets = new LogicalPosition[2];
      carets[index] = getEditor().getCaretModel().getLogicalPosition();
      carets[otherIndex] = new LogicalPosition(0, 0);
      return carets;
    }
  }
}
