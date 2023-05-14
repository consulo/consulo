/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.template.impl.editorActions;

import consulo.codeEditor.Editor;
import consulo.codeEditor.action.ActionPlan;
import consulo.codeEditor.action.TypedActionHandler;
import consulo.codeEditor.action.TypedActionHandlerEx;
import consulo.codeEditor.internal.OverrideTypedActionHandler;
import consulo.dataContext.DataContext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class TypedActionHandlerBase implements TypedActionHandlerEx, OverrideTypedActionHandler {
  @Nullable
  protected TypedActionHandler myOriginalHandler;

  @Override
  public void init(TypedActionHandler delegate) {
    myOriginalHandler = delegate;
  }

  @Override
  public void beforeExecute(@Nonnull Editor editor, char c, @Nonnull DataContext context, @Nonnull ActionPlan plan) {
    if (myOriginalHandler instanceof TypedActionHandlerEx) {
      ((TypedActionHandlerEx)myOriginalHandler).beforeExecute(editor, c, context, plan);
    }
  }
}
