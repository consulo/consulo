// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.memory.ui;

import consulo.application.Application;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.event.XDebugSessionListener;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.ui.ex.awt.DialogWrapper;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

public abstract class InstancesWindowBase extends DialogWrapper {
    protected static final int DEFAULT_WINDOW_WIDTH = 870;
    protected static final int DEFAULT_WINDOW_HEIGHT = 400;

    protected final String className;

    public InstancesWindowBase(XDebugSession session, String className) {
        super(session.getProject(), false);
        this.className = className;

        addWarningMessage(null);
        session.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionStopped() {
                Application.get().invokeLater(() -> close(OK_EXIT_CODE));
            }
        }, myDisposable);
        setModal(false);
    }

    protected void addWarningMessage(@Nullable String message) {
        setTitle(
            message == null
                ? XDebuggerLocalize.memoryViewInstancesDialogTitle(className)
                : XDebuggerLocalize.memoryViewInstancesDialogTitleWarning(className, message)
        );
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#org.jetbrains.debugger.memory.view.InstancesWindow";
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{new DialogWrapperExitAction(XDebuggerLocalize.memoryInstancesCloseText().get(), CLOSE_EXIT_CODE)};
    }
}
