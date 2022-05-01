/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.actionSystem.impl.actionholder;

import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;

public abstract class ActionRef<T extends AnAction> {
  private static ActionManager ourManager;
  static ActionManager getManager() {
    if (ourManager == null) {
      ourManager = ActionManager.getInstance();
    }
    return ourManager;
  }

  public static <T extends AnAction> ActionRef<T> fromAction(T action) {
    String id = getManager().getId(action);
    return id == null ? new SimpleActionRef<T>(action) : new IdActionRef<T>(id);
  }


  public abstract T getAction();
}
