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

import consulo.execution.unscramble.AnalyzeStacktraceUtil;
import consulo.ide.impl.idea.diagnostic.IdeErrorsDialog;
import consulo.dataContext.DataContext;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

/**
 * @author spleaner
 */
public class AnalyzeStacktraceOnErrorAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();

    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    final String message = dataContext.getData(IdeErrorsDialog.CURRENT_TRACE_KEY);
    if (message != null) {
      AnalyzeStacktraceUtil.addConsole(project, null, "<Stacktrace>", message);
    }
  }
}