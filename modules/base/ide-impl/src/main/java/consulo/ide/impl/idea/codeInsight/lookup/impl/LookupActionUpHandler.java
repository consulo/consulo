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
package consulo.ide.impl.idea.codeInsight.lookup.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.UISettings;
import consulo.codeEditor.Caret;
import consulo.dataContext.DataContext;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.ui.ex.action.IdeActions;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class LookupActionUpHandler extends LookupActionHandler {
  @Override
  protected void executeInLookup(LookupEx lookup, DataContext context, Caret caret) {
    if (!UISettings.getInstance().getCycleScrolling() && !lookup.isFocused() && lookup.getSelectedIndex() == 0) {
      myOriginalHandler.execute(lookup.getEditor(), caret, context);
      return;
    }
    executeUpOrDown(lookup, true);
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_MOVE_CARET_UP;
  }
}
