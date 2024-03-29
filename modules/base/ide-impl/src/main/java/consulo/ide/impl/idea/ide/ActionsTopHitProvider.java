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
package consulo.ide.impl.idea.ide;

import consulo.ui.ex.action.ActionManager;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;

import java.util.function.Consumer;

/**
 * @author Konstantin Bulenkov
 */
public abstract class ActionsTopHitProvider implements SearchTopHitProvider {
  @Override
  public void consumeTopHits(String pattern, Consumer<Object> collector, Project project) {
    final ActionManager actionManager = ActionManager.getInstance();
    for (String[] strings : getActionsMatrix()) {
      if (StringUtil.isBetween(pattern, strings[0], strings[1])) {
        for (int i = 2; i < strings.length; i++) {
          collector.accept(actionManager.getAction(strings[i]));
        }
      }
    }
  }

  protected abstract String[][] getActionsMatrix();
}
