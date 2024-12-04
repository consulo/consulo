// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime.log;

import consulo.remoteServer.agent.shared.TerminalListener;
import consulo.remoteServer.runtime.log.TerminalHandler;
import jakarta.annotation.Nonnull;

import javax.swing.*;

public abstract class TerminalHandlerBase extends LoggingHandlerBase implements TerminalHandler {
    private boolean myClosed = false;
    private TerminalListener.TtyResizeHandler myResizeHandler = (width, height) -> {
    };

    public TerminalHandlerBase(@Nonnull String presentableName) {
        super(presentableName);
    }

    @Override
    public abstract JComponent getComponent();

    @Override
    public boolean isClosed() {
        return myClosed;
    }

    @Override
    public void close() {
        myClosed = true;
    }

    public void setResizeHandler(@Nonnull TerminalListener.TtyResizeHandler resizeHandler) {
        myResizeHandler = resizeHandler;
    }

    protected @Nonnull TerminalListener.TtyResizeHandler getResizeHandler() {
        return myResizeHandler;
    }
}