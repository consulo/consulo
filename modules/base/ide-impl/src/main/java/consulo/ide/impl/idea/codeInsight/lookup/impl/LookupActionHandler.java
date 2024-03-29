// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.lookup.impl;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionManager;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.completion.CodeCompletionFeatures;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupFocusDegree;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public abstract class LookupActionHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
  protected EditorActionHandler myOriginalHandler;

  @Override
  public void init(@Nullable EditorActionHandler original) {
    myOriginalHandler = original;
  }

  @Override
  public boolean executeInCommand(@Nonnull Editor editor, DataContext dataContext) {
    return LookupManager.getActiveLookup(editor) == null;
  }

  @Override
  public void doExecute(@Nonnull Editor editor, Caret caret, DataContext dataContext) {
    LookupEx lookup = LookupManager.getActiveLookup(editor);
    if (lookup == null || !lookup.isAvailableToUser()) {
      Project project = editor.getProject();
      if (project != null && lookup != null) {
        LookupManager.getInstance(project).hideActiveLookup();
      }
      myOriginalHandler.execute(editor, caret, dataContext);
      return;
    }

    lookup.markSelectionTouched();
    executeInLookup(lookup, dataContext, caret);
  }

  protected abstract void executeInLookup(LookupEx lookup, DataContext context, @Nullable Caret caret);

  @Override
  public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
    LookupEx lookup = LookupManager.getActiveLookup(editor);
    return lookup != null || myOriginalHandler.isEnabled(editor, caret, dataContext);
  }

  protected static void executeUpOrDown(LookupEx lookup, boolean up) {
    if (!lookup.isFocused()) {
      boolean semiFocused = lookup.getLookupFocusDegree() == LookupFocusDegree.SEMI_FOCUSED;
      lookup.setFocusDegree(LookupFocusDegree.FOCUSED);
      if (!up && !semiFocused) {
        return;
      }
    }
    if (up) {
      lookup.moveUp();
    }
    else {
      lookup.moveDown();
    }
    lookup.markSelectionTouched();
    lookup.refreshUi(false, true);
  }

  protected static class UpDownInEditorHandler extends EditorActionHandler {
    private final boolean myUp;

    protected UpDownInEditorHandler(boolean up) {
      myUp = up;
    }

    @Override
    public boolean executeInCommand(@Nonnull Editor editor, DataContext dataContext) {
      return false;
    }

    @Override
    protected boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
      return LookupManager.getActiveLookup(editor) != null;
    }

    @Override
    protected void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_CONTROL_ARROWS);
      LookupEx lookup = LookupManager.getActiveLookup(editor);
      assert lookup != null : LookupDispose.getLastLookupDisposeTrace();
      lookup.hideLookup(true);
      EditorActionManager.getInstance().getActionHandler(myUp ? IdeActions.ACTION_EDITOR_MOVE_CARET_UP : IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN).execute(editor, caret, dataContext);
    }
  }

}
