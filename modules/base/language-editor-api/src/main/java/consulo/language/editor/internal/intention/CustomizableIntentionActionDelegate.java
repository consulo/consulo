// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.internal.intention;

import consulo.language.editor.intention.CustomizableIntentionAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;

/**
 * Intention action delegate that proxies CustomizableIntentionAction interface
 * to underlying action, if it inherits from CustomizableIntentionAction
 */
public interface CustomizableIntentionActionDelegate extends IntentionActionDelegate, CustomizableIntentionAction {
  @Override
  default boolean isShowSubmenu() {
    IntentionAction action = IntentionActionDelegate.unwrap(getDelegate());
    if (action instanceof CustomizableIntentionAction) {
      return ((CustomizableIntentionAction)action).isShowSubmenu();
    }
    return true;
  }

  @Override
  default boolean isSelectable() {
    IntentionAction action = IntentionActionDelegate.unwrap(getDelegate());
    if (action instanceof CustomizableIntentionAction) {
      return ((CustomizableIntentionAction)action).isSelectable();
    }
    return true;
  }

  @Override
  default boolean isShowIcon() {
    IntentionAction action = IntentionActionDelegate.unwrap(getDelegate());
    if (action instanceof CustomizableIntentionAction) {
      return ((CustomizableIntentionAction)action).isShowIcon();
    }
    return true;
  }
}
