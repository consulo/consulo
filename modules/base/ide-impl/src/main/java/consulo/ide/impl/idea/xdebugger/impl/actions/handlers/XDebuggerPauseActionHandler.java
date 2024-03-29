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
package consulo.ide.impl.idea.xdebugger.impl.actions.handlers;

import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.execution.debug.XDebuggerManager;
import jakarta.annotation.Nonnull;
import consulo.execution.debug.XDebugSession;
import consulo.ide.impl.idea.xdebugger.impl.XDebugSessionImpl;
import consulo.dataContext.DataContext;

/**
 * @author nik
*/
public class XDebuggerPauseActionHandler extends XDebuggerActionHandler {
  protected void perform(@Nonnull final XDebugSession session, final DataContext dataContext) {
    session.pause();
  }

  @Override
  public boolean isHidden(@Nonnull Project project, AnActionEvent event) {
    final XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
    return session == null || !((XDebugSessionImpl)session).isPauseActionSupported();
  }

  protected boolean isEnabled(@Nonnull final XDebugSession session, final DataContext dataContext) {
    return !session.isPaused();
  }
}
