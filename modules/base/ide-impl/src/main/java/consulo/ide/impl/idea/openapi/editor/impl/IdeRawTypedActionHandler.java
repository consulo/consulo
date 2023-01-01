/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.openapi.editor.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.action.ActionPlan;
import consulo.codeEditor.action.TypedActionHandler;
import consulo.codeEditor.action.TypedActionHandlerEx;
import consulo.codeEditor.internal.RawTypedActionHandler;
import consulo.dataContext.DataContext;

import javax.annotation.Nonnull;

@ExtensionImpl
public class IdeRawTypedActionHandler implements TypedActionHandlerEx, RawTypedActionHandler {
  private TypedActionHandler myDelegate;

  @Override
  public void init(TypedActionHandler delegate) {
    myDelegate = delegate;
  }

  @Override
  public void execute(@Nonnull Editor editor, char charTyped, @Nonnull DataContext dataContext) {
    editor.putUserData(EditorEx.DISABLE_CARET_SHIFT_ON_WHITESPACE_INSERTION, Boolean.TRUE);
    try {
      myDelegate.execute(editor, charTyped, dataContext);
    }
    finally {
      editor.putUserData(EditorEx.DISABLE_CARET_SHIFT_ON_WHITESPACE_INSERTION, null);
    }
  }

  @Override
  public void beforeExecute(@Nonnull Editor editor, char c, @Nonnull DataContext context, @Nonnull ActionPlan plan) {
    if (myDelegate instanceof TypedActionHandlerEx) ((TypedActionHandlerEx)myDelegate).beforeExecute(editor, c, context, plan);
  }
}
