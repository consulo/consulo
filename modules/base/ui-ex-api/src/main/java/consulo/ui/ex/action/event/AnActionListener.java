/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ui.ex.action.event;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

/**
 * @author max
 * @author Kirill Kalishev
 * @author Konstantin Bulenkov
 * @author 2002-05-15
 */
@TopicAPI(ComponentScope.APPLICATION)
public interface AnActionListener {
  default void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
  }

  /**
   * Note that using <code>dataContext</code> in implementing methods is unsafe - it could have been invalidated by the performed action.
   */
  default void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
  }

  default void beforeEditorTyping(char c, DataContext dataContext) {
  }

  @Deprecated
  @DeprecationInfo("Use AnActionListener")
  abstract class Adapter implements AnActionListener {
    @Override
    public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
    }

    @Override
    public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
    }

    @Override
    public void beforeEditorTyping(char c, DataContext dataContext) {
    }
  }
}
