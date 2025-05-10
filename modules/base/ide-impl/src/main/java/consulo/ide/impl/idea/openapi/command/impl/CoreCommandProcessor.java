// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.application.Application;
import consulo.component.messagebus.MessageBus;
import consulo.document.Document;
import consulo.ui.UIAccess;
import consulo.undoRedo.internal.CommandProcessorEx;
import consulo.undoRedo.internal.CommandToken;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandDescriptor;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import consulo.undoRedo.internal.builder.BaseCommandBuilder;
import consulo.undoRedo.internal.builder.BaseExecutableCommandBuilder;
import consulo.undoRedo.internal.builder.WrappableRunnableCommandBuilder;
import consulo.util.collection.Lists;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Stack;

public class CoreCommandProcessor extends CommandProcessorEx {
    protected interface WrappableStartableCommandBuilder<R, THIS extends WrappableStartableCommandBuilder<R, THIS>>
        extends StartableCommandBuilder<R, THIS>, WrappableRunnableCommandBuilder<R, THIS> {
    }

    protected class MyStartableCommandBuilder<R, THIS extends MyStartableCommandBuilder<R, THIS>>
        extends BaseExecutableCommandBuilder<R, THIS> implements WrappableStartableCommandBuilder<R, THIS> {

        @Override
        public CommandProcessor getCommandProcessor() {
            return CoreCommandProcessor.this;
        }

        @Override
        public Application getApplication() {
            return CoreCommandProcessor.this.myApplication;
        }

        @Override
        @RequiredUIAccess
        public CommandToken start() {
            return startCommand(build(EmptyRunnable.INSTANCE));
        }

        @Override
        @RequiredUIAccess
        @SuppressWarnings("unchecked")
        public ExecutionResult<R> execute(ThrowableSupplier<R, ? extends Throwable> executable) {
            SimpleReference<ExecutionResult<R>> result = SimpleReference.create();
            executeCommand(build(() -> result.set(super.execute(executable))));
            return result.isNull() ? new ExecutionResult((R)null) : result.get();
        }
    }

    private class Command implements CommandToken {
        private CommandDescriptor myDescriptor;

        Command(CommandDescriptor commandDescriptor) {
            this.myDescriptor = commandDescriptor;
        }

        @Override
        @RequiredUIAccess
        public void finish(@Nullable Throwable throwable) {
            finishCommand(this, throwable);
        }

        @Nullable
        @Override
        public Project getProject() {
            return myDescriptor.project();
        }

        public CommandDescriptor getDescriptor() {
            return myDescriptor;
        }

        public void setName(@Nonnull LocalizeValue name) {
            myDescriptor = new BaseCommandBuilder(myDescriptor).name(name).build(myDescriptor.command());
        }

        public void setGroupId(Object groupId) {
            myDescriptor = new BaseCommandBuilder(myDescriptor).groupId(groupId).build(myDescriptor.command());
        }
    }

    @Nonnull
    private final Application myApplication;

    protected Command myCurrentCommand;
    private final Stack<Command> myInterruptedCommands = new Stack<>();
    private final List<CommandListener> myListeners = Lists.newLockFreeCopyOnWriteList();
    private int myUndoTransparentCount;

    private final CommandListener eventPublisher;

    public CoreCommandProcessor(@Nonnull Application application) {
        myApplication = application;
        MessageBus messageBus = application.getMessageBus();
        messageBus.connect().subscribe(
            CommandListener.class,
            new CommandListener() {
                @Override
                public void commandStarted(@Nonnull CommandEvent event) {
                    for (CommandListener listener : myListeners) {
                        try {
                            listener.commandStarted(event);
                        }
                        catch (Throwable e) {
                            CommandLog.LOG.error(e);
                        }
                    }
                }

                @Override
                public void beforeCommandFinished(@Nonnull CommandEvent event) {
                    for (CommandListener listener : myListeners) {
                        try {
                            listener.beforeCommandFinished(event);
                        }
                        catch (Throwable e) {
                            CommandLog.LOG.error(e);
                        }
                    }
                }

                @Override
                public void commandFinished(@Nonnull CommandEvent event) {
                    for (CommandListener listener : myListeners) {
                        try {
                            listener.commandFinished(event);
                        }
                        catch (Throwable e) {
                            CommandLog.LOG.error(e);
                        }
                    }
                }

                @Override
                public void undoTransparentActionStarted() {
                    for (CommandListener listener : myListeners) {
                        try {
                            listener.undoTransparentActionStarted();
                        }
                        catch (Throwable e) {
                            CommandLog.LOG.error(e);
                        }
                    }
                }

                @Override
                public void beforeUndoTransparentActionFinished() {
                    for (CommandListener listener : myListeners) {
                        try {
                            listener.beforeUndoTransparentActionFinished();
                        }
                        catch (Throwable e) {
                            CommandLog.LOG.error(e);
                        }
                    }
                }

                @Override
                public void undoTransparentActionFinished() {
                    for (CommandListener listener : myListeners) {
                        try {
                            listener.undoTransparentActionFinished();
                        }
                        catch (Throwable e) {
                            CommandLog.LOG.error(e);
                        }
                    }
                }
            }
        );

        // will, command events occurred quite often, let's cache publisher
        eventPublisher = messageBus.syncPublisher(CommandListener.class);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> StartableCommandBuilder<T, ? extends StartableCommandBuilder<T, ?>> newCommand() {
        return new MyStartableCommandBuilder();
    }

    @RequiredUIAccess
    protected void executeCommand(CommandDescriptor commandDescriptor) {
        UIAccess.assertIsUIThread();
        Project project = commandDescriptor.project();
        if (project != null && project.isDisposed()) {
            CommandLog.LOG.error("Project " + project + " already disposed");
            return;
        }

        if (CommandLog.LOG.isDebugEnabled()) {
            CommandLog.LOG.debug(
                "executeCommand: " + commandDescriptor.command() +
                    ", name = " + commandDescriptor.name() +
                    ", groupId = " + commandDescriptor.groupId() +
                    ", in command = " + (myCurrentCommand != null) +
                    ", in transparent action = " + isUndoTransparentActionInProgress()
            );
        }

        if (myCurrentCommand != null) {
            commandDescriptor.command().run();
            return;
        }
        Throwable throwable = null;
        myCurrentCommand = new Command(commandDescriptor);
        try {
            fireCommandStarted();
            commandDescriptor.command().run();
        }
        catch (Throwable th) {
            throwable = th;
        }
        finally {
            if (myCurrentCommand != null) {
                myCurrentCommand.finish(throwable);
            }
        }
    }

    @Override
    @Nullable
    @RequiredUIAccess
    public CommandToken startCommand(CommandDescriptor commandDescriptor) {
        UIAccess.assertIsUIThread();
        Project project = commandDescriptor.project();
        if (project != null && project.isDisposed()) {
            return null;
        }

        if (CommandLog.LOG.isDebugEnabled()) {
            CommandLog.LOG.debug(
                "startCommand: name = " + commandDescriptor.name() +
                    ", groupId = " + commandDescriptor.groupId()
            );
        }

        if (myCurrentCommand != null) {
            return null;
        }

        myCurrentCommand = new Command(commandDescriptor);

        fireCommandStarted();

        return myCurrentCommand;
    }

    @RequiredUIAccess
    protected void finishCommand(@Nonnull CommandToken command, @Nullable Throwable throwable) {
        UIAccess.assertIsUIThread();
        CommandLog.LOG.assertTrue(myCurrentCommand != null, "no current command in progress");
        fireCommandFinished();
    }

    @RequiredUIAccess
    protected void fireCommandFinished() {
        UIAccess.assertIsUIThread();
        CommandDescriptor currentCommand = myCurrentCommand.getDescriptor();
        CommandEvent event = new CommandEvent(
            this,
            currentCommand.command(),
            currentCommand.name(),
            currentCommand.groupId(),
            currentCommand.project(),
            currentCommand.undoConfirmationPolicy(),
            currentCommand.shouldRecordActionForActiveDocument(),
            currentCommand.document()
        );
        CommandListener publisher = eventPublisher;
        try {
            publisher.beforeCommandFinished(event);
        }
        finally {
            myCurrentCommand = null;
            publisher.commandFinished(event);
        }
    }

    @Override
    @RequiredUIAccess
    public void enterModal() {
        UIAccess.assertIsUIThread();
        Command currentCommand = myCurrentCommand;
        myInterruptedCommands.push(currentCommand);
        if (currentCommand != null) {
            fireCommandFinished();
        }
    }

    @Override
    @RequiredUIAccess
    public void leaveModal() {
        UIAccess.assertIsUIThread();
        CommandLog.LOG.assertTrue(myCurrentCommand == null, "Command must not run: " + myCurrentCommand);

        myCurrentCommand = myInterruptedCommands.pop();
        if (myCurrentCommand != null) {
            fireCommandStarted();
        }
    }

    @Override
    @RequiredUIAccess
    public void setCurrentCommandName(@Nonnull LocalizeValue name) {
        UIAccess.assertIsUIThread();
        Command currentCommand = myCurrentCommand;
        assert currentCommand != null;
        currentCommand.setName(name);
    }

    @Override
    @RequiredUIAccess
    public void setCurrentCommandGroupId(Object groupId) {
        UIAccess.assertIsUIThread();
        Command currentCommand = myCurrentCommand;
        assert currentCommand != null;
        currentCommand.setGroupId(groupId);
    }

    @Override
    public boolean hasCurrentCommand() {
        return myCurrentCommand != null;
    }

    @Override
    @Nonnull
    public LocalizeValue getCurrentCommandNameValue() {
        Command currentCommand = myCurrentCommand;
        if (currentCommand != null) {
            return currentCommand.getDescriptor().name();
        }
        if (!myInterruptedCommands.isEmpty()) {
            Command command = myInterruptedCommands.peek();
            return command != null ? command.getDescriptor().name() : LocalizeValue.empty();
        }
        return LocalizeValue.empty();
    }

    @Override
    @Nullable
    public Object getCurrentCommandGroupId() {
        Command currentCommand = myCurrentCommand;
        if (currentCommand != null) {
            return currentCommand.getDescriptor().groupId();
        }
        if (!myInterruptedCommands.isEmpty()) {
            Command command = myInterruptedCommands.peek();
            return command != null ? command.getDescriptor().groupId() : null;
        }
        return null;
    }

    @Override
    @Nullable
    public Project getCurrentCommandProject() {
        Command currentCommand = myCurrentCommand;
        return currentCommand != null ? currentCommand.getProject() : null;
    }

    @Override
    public void addCommandListener(@Nonnull CommandListener listener) {
        myListeners.add(listener);
    }

    @Override
    public void removeCommandListener(@Nonnull CommandListener listener) {
        myListeners.remove(listener);
    }

    @Override
    @RequiredUIAccess
    public void runUndoTransparentAction(@Nonnull Runnable action) {
        if (CommandLog.LOG.isDebugEnabled()) {
            CommandLog.LOG.debug(
                "runUndoTransparentAction: " + action +
                    ", in command = " + (myCurrentCommand != null) +
                    ", in transparent action = " + isUndoTransparentActionInProgress()
            );
        }
        if (myUndoTransparentCount++ == 0) {
            eventPublisher.undoTransparentActionStarted();
        }
        try {
            action.run();
        }
        finally {
            if (myUndoTransparentCount == 1) {
                eventPublisher.beforeUndoTransparentActionFinished();
            }
            if (--myUndoTransparentCount == 0) {
                eventPublisher.undoTransparentActionFinished();
            }
        }
    }

    @Override
    public boolean isUndoTransparentActionInProgress() {
        return myUndoTransparentCount > 0;
    }

    @Override
    public void markCurrentCommandAsGlobal(@Nullable Project project) {
    }

    @Override
    public void addAffectedDocuments(@Nullable Project project, @Nonnull Document... docs) {
    }

    @Override
    public void addAffectedFiles(@Nullable Project project, @Nonnull VirtualFile... files) {
    }

    @RequiredUIAccess
    private void fireCommandStarted() {
        UIAccess.assertIsUIThread();
        CommandDescriptor currentCommand = myCurrentCommand.getDescriptor();
        CommandEvent event = new CommandEvent(
            this,
            currentCommand.command(),
            currentCommand.name(),
            currentCommand.groupId(),
            currentCommand.project(),
            currentCommand.undoConfirmationPolicy(),
            currentCommand.shouldRecordActionForActiveDocument(),
            currentCommand.document()
        );
        eventPublisher.commandStarted(event);
    }
}
