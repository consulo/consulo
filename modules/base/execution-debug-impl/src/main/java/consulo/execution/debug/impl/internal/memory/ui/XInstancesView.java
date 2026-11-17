/*
 * Copyright 2013-2026 consulo.io
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
package consulo.execution.debug.impl.internal.memory.ui;

import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.memory.TypeInfo;
import consulo.execution.debug.memory.XMemoryViewHandler;
import consulo.ui.ex.awt.ScrollPaneFactory;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.function.Consumer;

/**
 * The tree of the instances of a type - loaded when the root is expanded, again after each suspension.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
public class XInstancesView extends InstancesViewBase {
    /**
     * At most this many instances are shown.
     */
    private static final int MAX_TREE_NODE_COUNT = 2000;

    private final XDebugSession mySession;
    private final XMemoryViewHandler myHandler;
    private final TypeInfo myType;
    private final Consumer<@Nullable String> myWarningMessageConsumer;
    private final InstancesTree myTree;

    public XInstancesView(XDebugSession session,
                          XMemoryViewHandler handler,
                          TypeInfo type,
                          Consumer<@Nullable String> warningMessageConsumer) {
        super(new BorderLayout(), session);
        mySession = session;
        myHandler = handler;
        myType = type;
        myWarningMessageConsumer = warningMessageConsumer;
        myTree = new InstancesTree(session.getProject(), session.getDebugProcess().getEditorsProvider(), getValueMarkers(session), this::loadInstances);
        add(ScrollPaneFactory.createScrollPane(myTree), BorderLayout.CENTER);
    }

    @Override
    public InstancesTree getInstancesTree() {
        return myTree;
    }

    private void loadInstances() {
        XSuspendContext context = mySession.getSuspendContext();
        if (context == null) {
            return;
        }
        myHandler.computeInstances(myType, MAX_TREE_NODE_COUNT + 1, context).whenComplete((children, error) -> {
            mySession.getProject().getApplication().invokeLater(() -> {
                if (error != null) {
                    myTree.addChildren(XValueChildrenList.EMPTY, true);
                    myWarningMessageConsumer.accept(error.getMessage());
                    return;
                }
                if (children.size() > MAX_TREE_NODE_COUNT) {
                    XValueChildrenList shown = new XValueChildrenList();
                    for (int i = 0; i < MAX_TREE_NODE_COUNT; i++) {
                        shown.add(children.getName(i), children.getValue(i));
                    }
                    myTree.addChildren(shown, true);
                    myWarningMessageConsumer.accept(XDebuggerLocalize.memoryViewInstancesWarningNotAllLoaded(MAX_TREE_NODE_COUNT).get());
                }
                else {
                    myTree.addChildren(children, true);
                    myWarningMessageConsumer.accept(null);
                }
            });
        });
    }
}
