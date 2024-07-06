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
package consulo.ide.impl.idea.xdebugger.impl.ui.tree.actions;

import consulo.diff.DiffManager;
import consulo.diff.DiffRequestFactory;
import consulo.diff.request.DiffRequest;
import consulo.project.Project;
import consulo.ui.ex.awt.UIUtil;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XDebuggerTree;

/**
 * User: ksafonov
 */
public class XCompareWithClipboardAction extends XFetchValueActionBase {

  @Override
  protected void handle(final Project project, final String value, XDebuggerTree tree) {
    UIUtil.invokeLaterIfNeeded(() -> {
      DiffRequest request = DiffRequestFactory.getInstance().createClipboardVsValue(value);
      DiffManager.getInstance().showDiff(project, request);
    });
  }
}
