/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.execution.debug.frame.XNavigatable;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.util.AppUIUtil;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class XJumpToSourceActionBase extends XDebuggerTreeActionBase {
    protected XJumpToSourceActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    @Override
    protected void perform(XValueNodeImpl node, @Nonnull String nodeName, AnActionEvent e) {
        XValue value = node.getValueContainer();
        XNavigatable navigatable = sourcePosition -> {
            if (sourcePosition != null) {
                AppUIUtil.invokeOnEdt(() -> {
                    Project project = node.getTree().getProject();
                    if (project.isDisposed()) {
                        return;
                    }

                    sourcePosition.createNavigatable(project).navigate(true);
                });
            }
        };
        startComputingSourcePosition(value, navigatable);
    }

    protected abstract void startComputingSourcePosition(XValue value, XNavigatable navigatable);
}
