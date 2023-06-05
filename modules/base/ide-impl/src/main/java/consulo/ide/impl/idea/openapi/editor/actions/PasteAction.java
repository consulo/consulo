/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorCopyPasteHelper;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.action.EditorAction;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.util.dataholder.Key;

import java.awt.datatransfer.Transferable;
import java.util.function.Supplier;

/**
 * @author max
 * @since May 13, 2002
 */
public class PasteAction extends EditorAction {
  public static final Key<Supplier<Transferable>> TRANSFERABLE_PROVIDER = Key.create("PasteTransferableProvider");

  public PasteAction() {
    super(new Handler());
  }

  private static class Handler extends BasePasteHandler {
    @RequiredWriteAction
    @Override
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
      TextRange range = null;
      if (myTransferable != null) {
        TextRange[] ranges = EditorCopyPasteHelper.getInstance().pasteTransferable(editor, myTransferable);
        if (ranges != null && ranges.length == 1) {
          range = ranges[0];
        }
      }
      editor.putUserData(EditorEx.LAST_PASTED_REGION, range);
    }
  }
}
