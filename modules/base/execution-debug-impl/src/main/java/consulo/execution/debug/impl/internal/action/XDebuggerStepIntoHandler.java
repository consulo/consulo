// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.action;

import consulo.dataContext.DataContext;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.step.ForceSmartStepIntoSource;
import consulo.execution.debug.step.XSmartStepIntoHandler;
import consulo.execution.debug.step.XSmartStepIntoVariant;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class XDebuggerStepIntoHandler extends XDebuggerSmartStepIntoHandler {
    @Override
    protected boolean isEnabled(XDebugSession session, DataContext dataContext) {
        return session.isSuspended();
    }

    @Override
    public boolean isHidden(Project project, AnActionEvent event) {
        return false;
    }

    @Override
    protected <V extends XSmartStepIntoVariant> CompletableFuture<List<V>> computeVariants(XSmartStepIntoHandler<V> handler,
                                                                                           XSourcePosition position) {
        return handler.computeStepIntoVariants(position);
    }

    @Override
    @RequiredUIAccess
    protected <V extends XSmartStepIntoVariant> boolean handleSimpleCases(XSmartStepIntoHandler<V> handler,
                                                                          List<? extends V> variants,
                                                                          XDebugSession session) {
        if (variants.size() == 1) {
            V singleVariant = variants.get(0);
            if (singleVariant instanceof ForceSmartStepIntoSource forceSmartStepIntoSource && forceSmartStepIntoSource.needForceSmartStepInto()) {
                return super.handleSimpleCases(handler, variants, session);
            }
        }

        if (variants.size() < 2) {
            session.stepInto();
            return true;
        }
        return false;
    }
}
