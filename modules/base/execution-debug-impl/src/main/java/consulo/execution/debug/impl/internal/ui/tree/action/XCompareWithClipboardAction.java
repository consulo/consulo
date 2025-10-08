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
package consulo.execution.debug.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.diff.DiffManager;
import consulo.diff.DiffRequestFactory;
import consulo.diff.request.DiffRequest;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.project.Project;

/**
 * @author ksafonov
 */
@ActionImpl(id = "XDebugger.CompareValueWithClipboard")
public class XCompareWithClipboardAction extends XFetchValueActionBase {
    public XCompareWithClipboardAction() {
        super(XDebuggerLocalize.actionCompareValueWithClipboardText(), XDebuggerLocalize.actionCompareValueWithClipboardDescription());
    }

    @Override
    protected void handle(Project project, String value, XDebuggerTree tree) {
        project.getUIAccess().give(() -> {
            DiffRequest request = DiffRequestFactory.getInstance().createClipboardVsValue(value);
            DiffManager.getInstance().showDiff(project, request);
        });
    }
}
