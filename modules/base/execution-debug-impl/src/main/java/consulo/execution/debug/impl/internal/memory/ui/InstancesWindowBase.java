// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.memory.ui;

import consulo.application.ApplicationManager;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.event.XDebugSessionListener;
import consulo.ui.ex.awt.DialogWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public abstract class InstancesWindowBase extends DialogWrapper {

    protected static final int DEFAULT_WINDOW_WIDTH = 870;
    protected static final int DEFAULT_WINDOW_HEIGHT = 400;

    protected final String className;

    public InstancesWindowBase(@Nonnull XDebugSession session,
                               @Nonnull String className) {
        super(session.getProject(), false);
        this.className = className;

        addWarningMessage(null);
        session.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionStopped() {
                ApplicationManager.getApplication().invokeLater(() -> close(OK_EXIT_CODE));
            }
        }, myDisposable);
        setModal(false);
    }

    protected void addWarningMessage(@Nullable String message) {
        setTitle(message == null ?
            XDebuggerBundle.message("memory.view.instances.dialog.title", className) :
            XDebuggerBundle.message("memory.view.instances.dialog.title.warning", className, message));
    }

    @Nonnull
    @Override
    protected String getDimensionServiceKey() {
        return "#org.jetbrains.debugger.memory.view.InstancesWindow";
    }

    @Override
    @Nonnull
    protected Action[] createActions() {
        return new Action[]{new DialogWrapperExitAction(XDebuggerBundle.message("memory.instances.close.text"), CLOSE_EXIT_CODE)};
    }
}
