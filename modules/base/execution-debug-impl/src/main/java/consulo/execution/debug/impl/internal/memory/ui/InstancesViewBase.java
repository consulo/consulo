// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.memory.ui;

import consulo.application.ApplicationManager;
import consulo.disposer.Disposable;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.event.XDebugSessionListener;
import consulo.execution.debug.frame.XValueMarkers;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTreeState;
import consulo.execution.debug.memory.InstancesProvider;
import consulo.ui.ex.awt.JBPanel;
import jakarta.annotation.Nonnull;

import java.awt.*;

public abstract class InstancesViewBase extends JBPanel implements Disposable {
    private final InstancesProvider myInstancesProvider;

    public InstancesViewBase(@Nonnull LayoutManager layout, @Nonnull XDebugSession session, InstancesProvider instancesProvider) {
        super(layout);

        myInstancesProvider = instancesProvider;
        session.addSessionListener(new MySessionListener(), this);
    }

    protected XValueMarkers<?, ?> getValueMarkers(@Nonnull XDebugSession session) {
        return session instanceof XDebugSessionImpl
            ? ((XDebugSessionImpl) session).getValueMarkers()
            : null;
    }

    protected abstract InstancesTree getInstancesTree();

    @Override
    public void dispose() {
    }

    public InstancesProvider getInstancesProvider() {
        return myInstancesProvider;
    }

    private class MySessionListener implements XDebugSessionListener {
        private volatile XDebuggerTreeState myTreeState = null;

        @Override
        public void sessionResumed() {
            ApplicationManager.getApplication().invokeLater(() -> {
                myTreeState = XDebuggerTreeState.saveState(getInstancesTree());

                getInstancesTree().setInfoMessage(
                    "The application is running");
            });
        }

        @Override
        public void sessionPaused() {
            ApplicationManager.getApplication().invokeLater(() -> {
                XDebuggerTreeState state = myTreeState;
                InstancesTree tree = getInstancesTree();
                if (state == null) {
                    tree.rebuildTree(InstancesTree.RebuildPolicy.RELOAD_INSTANCES);
                }
                else {
                    tree.rebuildTree(InstancesTree.RebuildPolicy.RELOAD_INSTANCES, state);
                }
            });
        }
    }
}
