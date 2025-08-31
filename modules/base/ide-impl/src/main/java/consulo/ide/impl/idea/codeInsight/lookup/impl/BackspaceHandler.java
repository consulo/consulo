// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.lookup.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInsight.completion.CompletionProgressIndicator;
import consulo.ide.impl.idea.codeInsight.completion.impl.CompletionServiceImpl;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public class BackspaceHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
  private EditorActionHandler myOriginalHandler;

  @Override
  public void doExecute(@Nonnull Editor editor, Caret caret, DataContext dataContext) {
    LookupEx lookup = LookupManager.getActiveLookup(editor);
    if (lookup == null) {
      myOriginalHandler.execute(editor, caret, dataContext);
      return;
    }

    int hideOffset = lookup.getLookupStart();
    int originalStart = lookup.getLookupOriginalStart();
    if (originalStart >= 0 && originalStart <= hideOffset) {
      hideOffset = originalStart - 1;
    }

    truncatePrefix(dataContext, lookup, myOriginalHandler, hideOffset, caret);
  }

  static void truncatePrefix(DataContext dataContext, LookupEx lookup, EditorActionHandler handler, int hideOffset, Caret caret) {
    Editor editor = lookup.getEditor();
    if (!lookup.performGuardedChange(() -> handler.execute(editor, caret, dataContext))) {
      return;
    }

    CompletionProgressIndicator process = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
    lookup.truncatePrefix(process == null || !process.isAutopopupCompletion(), hideOffset);
  }

  @Override
  public void init(@Nullable EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_BACKSPACE;
  }
}
