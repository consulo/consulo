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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

public class CloseAction extends AnAction implements DumbAware {

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setIcon(e.isFromActionToolbar() ? AllIcons.Actions.Cancel : null);

    CloseTarget closeTarget = e.getData(CloseTarget.KEY);
    e.getPresentation().setEnabled(closeTarget != null);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    e.getData(CloseTarget.KEY).close();
  }

  public interface CloseTarget {
    Key<CloseTarget> KEY = Key.create("GenericClosable");

    void close();
  }
}
