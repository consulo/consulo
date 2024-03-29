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
package consulo.ide.impl.idea.openapi.keymap.ex;

import consulo.component.persist.scheme.SchemeManager;
import consulo.ide.impl.idea.openapi.keymap.impl.KeymapImpl;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;

import java.util.Set;

public abstract class KeymapManagerEx extends KeymapManager {
  public static KeymapManagerEx getInstanceEx(){
    return (KeymapManagerEx)getInstance();
  }

  /**
   * @return all available keymaps. The method return an empty array if no
   * keymaps are available.
   */
  @Override
  public abstract Keymap[] getAllKeymaps();

  public abstract void setActiveKeymap(Keymap activeKeymap);

  /**
   * Instructs the manager that one action should use shortcut of another one (<code>'use-shortcut-of'</code> attribute at
   * action's config located at plugin.xml).
   *
   * @param sourceActionId  if of the action which shortcut should be used for the 'target action'
   * @param targetActionId  id of the action which should use shortcut of the 'source action'
   */
  public abstract void bindShortcuts(String sourceActionId, String targetActionId);
  public abstract void unbindShortcuts(String targetActionId);

  public abstract Set<String> getBoundActions();
  public abstract String getActionBinding(String actionId);

  public abstract SchemeManager<Keymap, KeymapImpl> getSchemeManager();
}
