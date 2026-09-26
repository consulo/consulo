// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.toolbar.floating;

import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.util.lang.lazy.LazyValue;

import java.util.function.Supplier;

public abstract class AbstractFloatingToolbarProvider implements FloatingToolbarProvider {
    private static final Logger LOG = Logger.getInstance(FloatingToolbarProvider.class);

    private final Supplier<ActionGroup> myActionGroup;

    protected AbstractFloatingToolbarProvider(String actionGroupId) {
        myActionGroup = LazyValue.atomicNotNull(() -> resolveActionGroup(actionGroupId));
    }

    @Override
    public ActionGroup getActionGroup() {
        return myActionGroup.get();
    }

    @Override
    @RequiredUIAccess
    public void register(DataContext dataContext, FloatingToolbarComponent component, Disposable parentDisposable) {
    }

    private static ActionGroup resolveActionGroup(String actionGroupId) {
        ActionManager actionManager = ActionManager.getInstance();
        AnAction action = actionManager.getAction(actionGroupId);
        if (action instanceof ActionGroup actionGroup) {
            return actionGroup;
        }
        LOG.warn("Cannot initialize action group using (" + (action == null ? null : action.getClass()) + ")");
        DefaultActionGroup defaultActionGroup = new DefaultActionGroup();
        actionManager.registerAction(actionGroupId, defaultActionGroup);
        return defaultActionGroup;
    }
}
