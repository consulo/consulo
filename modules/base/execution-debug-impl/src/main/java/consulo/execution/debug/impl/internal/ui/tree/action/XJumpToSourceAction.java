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
package consulo.execution.debug.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.frame.XNavigatable;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ActionImpl(id = XDebuggerActions.JUMP_TO_SOURCE)
public class XJumpToSourceAction extends XJumpToSourceActionBase {
    public XJumpToSourceAction() {
        super(XDebuggerLocalize.actionJumpToSourceText(), XDebuggerLocalize.actionJumpToSourceDescription());
    }

    @Override
    protected void startComputingSourcePosition(XValue value, XNavigatable navigatable) {
        value.computeSourcePosition(navigatable);
    }

    @Override
    protected boolean isEnabled(@Nonnull XValueNodeImpl node, @Nonnull AnActionEvent e) {
        return super.isEnabled(node, e) && node.getValueContainer().canNavigateToSource();
    }
}
