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
package consulo.execution.debug.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.impl.internal.frame.action.XWatchesTreeActionBase;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.node.WatchNode;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.project.Project;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.ex.CopyPasteManager;
import consulo.util.collection.ContainerUtil;

import java.util.List;

/**
 * @author nik
 */
@ActionImpl(id = XDebuggerActions.COPY_VALUE)
public class XCopyValueAction extends XFetchValueActionBase {
    public static final DataTransferType<List<XExpression>> WATCH_EXPRESSIONS = DataTransferType.create("consulo.debugger.watchExpressions");

    public XCopyValueAction() {
        super(XDebuggerLocalize.actionCopyValueText(), XDebuggerLocalize.actionCopyValueDescription());
    }

    @Override
    protected void handle(Project project, String value, XDebuggerTree tree) {
        if (tree == null) {
            return;
        }
        List<? extends WatchNode> watchNodes = XWatchesTreeActionBase.getSelectedNodes(tree, WatchNode.class);
        if (watchNodes.isEmpty()) {
            CopyPasteManager.getInstance().setText(value);
        }
        else {
            CopyPasteManager.getInstance().setContents(DataTransfer.builder()
                .put(DataTransferType.TEXT, value)
                .put(WATCH_EXPRESSIONS, List.copyOf(ContainerUtil.map(watchNodes, WatchNode::getExpression)))
                .build());
        }
    }
}
