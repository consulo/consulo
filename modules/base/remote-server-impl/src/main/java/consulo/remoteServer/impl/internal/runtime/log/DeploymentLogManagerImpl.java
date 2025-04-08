// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime.log;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.remoteServer.runtime.log.LoggingHandler;
import consulo.remoteServer.runtime.log.TerminalHandler;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeploymentLogManagerImpl implements DeploymentLogManager {
    private final LoggingHandlerImpl myMainLoggingHandler;
    private final Project myProject;
    private final List<LoggingHandlerBase> myAdditionalLoggingHandlers = new ArrayList<>();
    private final Runnable myChangeListener;

    private final AtomicBoolean myLogsDisposed = new AtomicBoolean(false);
    private final Disposable myLogsDisposable;
    private boolean myMainHandlerVisible = false;

    public DeploymentLogManagerImpl(@Nonnull Project project, @Nonnull Runnable changeListener) {
        myProject = project;
        myChangeListener = changeListener;
        myMainLoggingHandler = new LoggingHandlerImpl.Colored(null, project);
        myLogsDisposable = Disposable.newDisposable();
        Disposer.register(myLogsDisposable, myMainLoggingHandler);
        Disposer.register(project, this::disposeLogs);
    }

    @Override
    public @Nonnull Project getProject() {
        return myProject;
    }

    public DeploymentLogManagerImpl withMainHandlerVisible(boolean mainHandlerVisible) {
        myMainHandlerVisible = mainHandlerVisible;
        return this;
    }

    public boolean isMainHandlerVisible() {
        return myMainHandlerVisible;
    }

    @Override
    public @Nonnull LoggingHandlerImpl getMainLoggingHandler() {
        return myMainLoggingHandler;
    }

    @Override
    public @Nonnull LoggingHandler addAdditionalLog(@Nonnull String presentableName) {
        synchronized (myLogsDisposed) {
            if (myLogsDisposed.get()) {
                throw new IllegalStateException("Already disposed, can't add " + presentableName);
            }

            LoggingHandlerImpl handler = new LoggingHandlerImpl.Colored(presentableName, myProject);
            addAdditionalLoggingHandler(handler);
            return handler;
        }
    }

    @Override
    public void removeAdditionalLog(@Nonnull String presentableName) {
        synchronized (myAdditionalLoggingHandlers) {
            myAdditionalLoggingHandlers.removeIf(next -> presentableName.equals(next.getPresentableName()));
        }
        myChangeListener.run();
    }

    public @Nonnull LoggingHandler findOrCreateAdditionalLog(@Nonnull String presentableName) {
        synchronized (myAdditionalLoggingHandlers) {
            for (LoggingHandlerBase next : myAdditionalLoggingHandlers) {
                if (next instanceof LoggingHandler && presentableName.equals(next.getPresentableName())) {
                    return (LoggingHandler)next;
                }
            }
            return addAdditionalLog(presentableName);
        }
    }

    @Override
    public @Nullable TerminalHandler addTerminal(
        @Nonnull @Nls String presentableName,
        InputStream terminalOutput,
        OutputStream terminalInput
    ) {
        synchronized (myLogsDisposed) {
            if (myLogsDisposed.get()) {
                return null;
            }
            TerminalHandlerBase handler =
                CloudTerminalProvider.getInstance().createTerminal(presentableName, myProject, terminalOutput, terminalInput);
            addAdditionalLoggingHandler(handler);
            return handler;
        }
    }

    //private static CloudTerminalProvider getTerminalProvider() {
    //    CloudTerminalProvider.getInstance()
    //
    //    CloudTerminalProvider terminalProvider = ArrayUtil.getFirstElement(CloudTerminalProvider.EP_NAME.getExtensions());
    //    return terminalProvider != null ? terminalProvider : ConsoleTerminalHandlerImpl.PROVIDER;
    //}

    @Override
    public boolean isTtySupported() {
        return CloudTerminalProvider.getInstance().isTtySupported();
    }

    private void addAdditionalLoggingHandler(LoggingHandlerBase loggingHandler) {
        Disposer.register(myLogsDisposable, loggingHandler);
        synchronized (myAdditionalLoggingHandlers) {
            myAdditionalLoggingHandlers.add(loggingHandler);
        }
        myChangeListener.run();
    }

    /*
      FIXME: memory leak. We need to remove closed handlers
     */
    public @Nonnull List<LoggingHandlerBase> getAdditionalLoggingHandlers() {
        List<LoggingHandlerBase> result;
        synchronized (myAdditionalLoggingHandlers) {
            result = new ArrayList<>(myAdditionalLoggingHandlers);
        }
        return result;
    }

    public void disposeLogs() {
        synchronized (myLogsDisposed) {
            if (!myLogsDisposed.getAndSet(true)) {
                Disposer.dispose(myLogsDisposable);
            }
        }
    }
}
