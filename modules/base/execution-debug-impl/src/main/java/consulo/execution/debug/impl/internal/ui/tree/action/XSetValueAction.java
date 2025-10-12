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
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.impl.internal.ui.tree.SetValueInplaceEditor;
import consulo.execution.debug.impl.internal.ui.tree.node.WatchNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ActionImpl(id = XDebuggerActions.SET_VALUE)
public class XSetValueAction extends XDebuggerTreeActionBase {
    public XSetValueAction() {
        super(XDebuggerLocalize.actionSetValueText(), XDebuggerLocalize.actionSetValueDescription());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        XValueNodeImpl node = getSelectedNode(e.getDataContext());
        Presentation presentation = e.getPresentation();
        if (node instanceof WatchNode) {
            presentation.setEnabledAndVisible(false);
        }
        else {
            presentation.setVisible(true);
        }
    }

    @Override
    protected boolean isEnabled(@Nonnull XValueNodeImpl node, @Nonnull AnActionEvent e) {
        return super.isEnabled(node, e) && node.getValueContainer().getModifier() != null;
    }

    @Override
    protected void perform(XValueNodeImpl node, @Nonnull String nodeName, AnActionEvent e) {
        SetValueInplaceEditor.show(node, nodeName);
    }
}
