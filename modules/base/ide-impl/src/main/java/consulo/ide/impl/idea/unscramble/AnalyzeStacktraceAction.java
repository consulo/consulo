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

package consulo.ide.impl.idea.unscramble;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;

/**
 * @author yole
 */
@ActionImpl(id = "AnalyzeStacktrace", parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_ANALYZE)))
public class AnalyzeStacktraceAction extends AnAction implements DumbAware {
  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getDataContext().getData(Project.KEY);
    UnscrambleDialog dialog = new UnscrambleDialog(project, null);
    dialog.showAsync();
  }

  @RequiredUIAccess
  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(Project.KEY) != null);
  }
}
