/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.keymap.KeymapManager;
import jakarta.annotation.Nonnull;

/**
 * Please do not use this class outside impl package!!!
 * Please do not use this class even if you managed to make it public!!!
 * Thank you in advance. 
 *    The UI Engineers. 
 */ 
final class ProxyShortcutSet implements ShortcutSet {
  private final String myActionId;

  public ProxyShortcutSet(String actionId) {
    myActionId = actionId;
  }

  @Nonnull
  public Shortcut[] getShortcuts() {
    return KeymapManager.getInstance().getActiveKeymap().getShortcuts(myActionId);
  }
}
