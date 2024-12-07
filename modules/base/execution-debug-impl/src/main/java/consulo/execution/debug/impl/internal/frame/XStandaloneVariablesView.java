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
package consulo.execution.debug.impl.internal.frame;

import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.XStackFrame;
import consulo.project.Project;
import consulo.project.ui.util.AppUIUtil;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class XStandaloneVariablesView extends XVariablesViewBase {
  private final XStackFrame myStackFrame;

  public XStandaloneVariablesView(@Nonnull Project project, @Nonnull XDebuggerEditorsProvider editorsProvider, @Nonnull XStackFrame stackFrame) {
    super(project, editorsProvider, null);
    myStackFrame = stackFrame;
    buildTreeAndRestoreState(stackFrame);
  }

  public void rebuildView() {
    AppUIUtil.invokeLaterIfProjectAlive(getTree().getProject(), () -> {
      saveCurrentTreeState(myStackFrame);
      buildTreeAndRestoreState(myStackFrame);
    });
  }

  @Override
  public void processSessionEvent(@Nonnull SessionEvent event, @Nonnull XDebugSession session) {
  }
}
