// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.lookup.impl;

import consulo.ide.impl.idea.codeInsight.completion.CompletionProgressIndicator;
import consulo.ide.impl.idea.codeInsight.completion.impl.CompletionServiceImpl;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import javax.annotation.Nonnull;

public class BackspaceHandler extends EditorActionHandler {
  private final EditorActionHandler myOriginalHandler;

  public BackspaceHandler(EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Override
  public void doExecute(@Nonnull final Editor editor, Caret caret, final DataContext dataContext) {
    LookupImpl lookup = (LookupImpl)LookupManager.getActiveLookup(editor);
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

  static void truncatePrefix(final DataContext dataContext, LookupImpl lookup, final EditorActionHandler handler, final int hideOffset, final Caret caret) {
    final Editor editor = lookup.getEditor();
    if (!lookup.performGuardedChange(() -> handler.execute(editor, caret, dataContext))) {
      return;
    }

    final CompletionProgressIndicator process = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
    lookup.truncatePrefix(process == null || !process.isAutopopupCompletion(), hideOffset);
  }
}
