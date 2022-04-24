/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.language.editor.internal;

import consulo.document.util.TextRange;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.QuickFixAction;
import consulo.language.editor.intention.QuickFixActionRegistrar;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

public class QuickFixActionRegistrarImpl implements QuickFixActionRegistrar {
  private final HighlightInfo myInfo;

  public QuickFixActionRegistrarImpl(@Nullable HighlightInfo info) {
    myInfo = info;
  }

  @Override
  public void register(@Nonnull IntentionAction action) {
    QuickFixAction.registerQuickFixAction(myInfo, action);
  }

  @Override
  public void register(@Nonnull TextRange fixRange, @Nonnull IntentionAction action, HighlightDisplayKey key) {
    QuickFixAction.registerQuickFixAction(myInfo, fixRange, action, key);
  }

  @Override
  public void unregister(@Nonnull Predicate<IntentionAction> condition) {
    if (myInfo != null) {
      myInfo.unregisterQuickFix(condition);
    }
  }
}
