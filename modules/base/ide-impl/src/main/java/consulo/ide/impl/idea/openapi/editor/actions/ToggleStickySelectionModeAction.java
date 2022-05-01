/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.EditorEx;

/**
 * Allows to toggle {@link EditorEx#isStickySelection() sticky selection} for editors.
 * <p/>
 * Thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 4/20/11 3:28 PM
 */
public class ToggleStickySelectionModeAction extends EditorAction {

  public ToggleStickySelectionModeAction() {
    super(new Handler());
  }

  static class Handler extends EditorActionHandler {
    @Override
    public void execute(Editor editor, DataContext dataContext) {
      if (!(editor instanceof EditorEx)) {
        return;
      }
      
      EditorEx ex = (EditorEx)editor;
      ex.setStickySelection(!ex.isStickySelection());
    }
  }
}
