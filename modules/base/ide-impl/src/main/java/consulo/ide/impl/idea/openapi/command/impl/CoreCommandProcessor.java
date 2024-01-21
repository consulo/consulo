// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.application.Application;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.*;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import consulo.undoRedo.internal.CommandProcessorEx;
import consulo.undoRedo.internal.CommandToken;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.util.Stack;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class CoreCommandProcessor implements CommandProcessorEx {
  private static class CommandDescriptor implements CommandToken, CommandExecutor {
    @Nonnull
    public final Command myCommand;
    public final Project myProject;
    public String myName;
    public Object myGroupId;
    public final Document myDocument;
    @Nonnull
    public final UndoConfirmationPolicy myUndoConfirmationPolicy;
    public final boolean myShouldRecordActionForActiveDocument;

    CommandDescriptor(@Nonnull Command command,
                      Project project,
                      String name,
                      Object groupId,
                      @Nonnull UndoConfirmationPolicy undoConfirmationPolicy,
                      boolean shouldRecordActionForActiveDocument,
                      Document document) {
      myCommand = command;
      myProject = project;
      myName = name;
      myGroupId = groupId;
      myUndoConfirmationPolicy = undoConfirmationPolicy;
      myShouldRecordActionForActiveDocument = shouldRecordActionForActiveDocument;
      myDocument = document;
    }

    @Override
    public Project getProject() {
      return myProject;
    }

    @Override
    public String toString() {
      return "'" + myName + "', group: '" + myGroupId + "'";
    }

    @Override
    public void execute(@Nonnull Runnable command) {
      command.run();
      // TODO
    }
  }

  @Nonnull
  private final Application myApplication;

  protected CommandDescriptor myCurrentCommand;
  private final Stack<CommandDescriptor> myInterruptedCommands = new Stack<>();
  private int myUndoTransparentCount;

  private final CommandListener eventPublisher;

  public CoreCommandProcessor(@Nonnull Application application) {
    myApplication = application;

    // will, command events occurred quite often, let's cache publisher
    eventPublisher = application.getMessageBus().syncPublisher(CommandListener.class);
  }

  @Override
  @Nonnull
  @RequiredUIAccess
  public <V> CompletableFuture<V> executeCommandAsync(@Nonnull CommandInfo commandInfo,
                                                      @Nonnull CommandRunnableAsync<V> command) {
    @Nonnull UndoConfirmationPolicy confirmationPolicy = commandInfo.confirmationPolicy();
    @Nullable Document document = commandInfo.document();
    @Nullable Object groupId = commandInfo.groupId();
    @Nullable Project project = commandInfo.project();
    @Nullable String name = commandInfo.name();
    boolean shouldRecordCommandForActiveDocument = commandInfo.shouldRecordCommandForActiveDocument();

    myApplication.assertIsDispatchThread();

    if (project != null && project.isDisposed()) {
      String message = "Project " + project + " already disposed";
      CommandLog.LOG.error(message);
      return CompletableFuture.failedFuture(new CancellationException(message));
    }

    if (CommandLog.LOG.isDebugEnabled()) {
      CommandLog.LOG.debug("executeCommand: " + command + ", name = " + name + ", groupId = " + groupId +
                             ", in command = " + (myCurrentCommand != null) +
                             ", in transparent action = " + isUndoTransparentActionInProgress());
    }

    CommandDescriptor currentCommand = myCurrentCommand;
    if (currentCommand != null) {
      return command.execute(currentCommand);
    }

    UIAccess uiAccess = UIAccess.current();

    CommandDescriptor descriptor =
      new CommandDescriptor(command, project, name, groupId, confirmationPolicy, shouldRecordCommandForActiveDocument, document);
    myCurrentCommand = descriptor;

    fireCommandStarted();

    CompletableFuture<V> future = command.execute(descriptor);
    future.whenCompleteAsync((v, throwable) -> finishCommand(descriptor, throwable), uiAccess);
    return future;
  }

  @Override
  @RequiredUIAccess
  public void executeCommand(CommandInfo commandInfo, CommandRunnable command) {
    @Nonnull UndoConfirmationPolicy confirmationPolicy = commandInfo.confirmationPolicy();
    @Nullable Document document = commandInfo.document();
    @Nullable Object groupId = commandInfo.groupId();
    @Nullable Project project = commandInfo.project();
    @Nullable String name = commandInfo.name();
    boolean shouldRecordCommandForActiveDocument = commandInfo.shouldRecordCommandForActiveDocument();

    myApplication.assertIsDispatchThread();
    if (project != null && project.isDisposed()) {
      CommandLog.LOG.error("Project " + project + " already disposed");
      return;
    }

    if (CommandLog.LOG.isDebugEnabled()) {
      CommandLog.LOG.debug("executeCommand: " + command + ", name = " + name + ", groupId = " + groupId +
                             ", in command = " + (myCurrentCommand != null) +
                             ", in transparent action = " + isUndoTransparentActionInProgress());
    }

    if (myCurrentCommand != null) {
      command.run();
      return;
    }
    Throwable throwable = null;
    try {
      myCurrentCommand =
        new CommandDescriptor(command, project, name, groupId, confirmationPolicy, shouldRecordCommandForActiveDocument, document);
      fireCommandStarted();
      command.run();
    }
    catch (Throwable th) {
      throwable = th;
    }
    finally {
      finishCommand(myCurrentCommand, throwable);
    }
  }

  @Override
  @Nullable
  @RequiredUIAccess
  public CommandToken startCommand(@Nullable final Project project,
                                   @Nls final String name,
                                   @Nullable final Object groupId,
                                   @Nonnull final UndoConfirmationPolicy undoConfirmationPolicy) {
    myApplication.assertIsDispatchThread();
    if (project != null && project.isDisposed()) return null;

    if (CommandLog.LOG.isDebugEnabled()) {
      CommandLog.LOG.debug("startCommand: name = " + name + ", groupId = " + groupId);
    }

    if (myCurrentCommand != null) {
      return null;
    }

    Document document =
      groupId instanceof Document ? (Document)groupId : (groupId instanceof SimpleReference && ((SimpleReference)groupId).get() instanceof Document ? (Document)((SimpleReference)groupId)
        .get() : null);
    myCurrentCommand = new CommandDescriptor((CommandRunnable)() -> {
    }, project, name, groupId, undoConfirmationPolicy, true, document);
    fireCommandStarted();
    return myCurrentCommand;
  }

  @Override
  @RequiredUIAccess
  public void finishCommand(@Nonnull final CommandToken command, @Nullable Throwable throwable) {
    myApplication.assertIsDispatchThread();
    CommandLog.LOG.assertTrue(myCurrentCommand != null, "no current command in progress");
    fireCommandFinished();
  }

  @RequiredUIAccess
  protected void fireCommandFinished() {
    myApplication.assertIsDispatchThread();
    CommandDescriptor currentCommand = myCurrentCommand;
    CommandEvent event = new CommandEvent(this,
                                          currentCommand.myCommand,
                                          currentCommand.myName,
                                          currentCommand.myGroupId,
                                          currentCommand.myProject,
                                          currentCommand.myUndoConfirmationPolicy,
                                          currentCommand.myShouldRecordActionForActiveDocument,
                                          currentCommand.myDocument);
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
    myApplication.assertIsDispatchThread();
    CommandDescriptor currentCommand = myCurrentCommand;
    myInterruptedCommands.push(currentCommand);
    if (currentCommand != null) {
      fireCommandFinished();
    }
  }

  @Override
  @RequiredUIAccess
  public void leaveModal() {
    myApplication.assertIsDispatchThread();
    CommandLog.LOG.assertTrue(myCurrentCommand == null, "Command must not run: " + myCurrentCommand);

    myCurrentCommand = myInterruptedCommands.pop();
    if (myCurrentCommand != null) {
      fireCommandStarted();
    }
  }

  @Override
  @RequiredUIAccess
  public void setCurrentCommandName(String name) {
    myApplication.assertIsDispatchThread();
    CommandDescriptor currentCommand = myCurrentCommand;
    CommandLog.LOG.assertTrue(currentCommand != null);
    currentCommand.myName = name;
  }

  @Override
  @RequiredUIAccess
  public void setCurrentCommandGroupId(Object groupId) {
    myApplication.assertIsDispatchThread();
    CommandDescriptor currentCommand = myCurrentCommand;
    CommandLog.LOG.assertTrue(currentCommand != null);
    currentCommand.myGroupId = groupId;
  }

  @Override
  public boolean hasCurrentCommand() {
    return myCurrentCommand != null;
  }

  @Override
  @Nullable
  public String getCurrentCommandName() {
    CommandDescriptor currentCommand = myCurrentCommand;
    if (currentCommand != null) return currentCommand.myName;
    if (!myInterruptedCommands.isEmpty()) {
      final CommandDescriptor command = myInterruptedCommands.peek();
      return command != null ? command.myName : null;
    }
    return null;
  }

  @Override
  @Nullable
  public Object getCurrentCommandGroupId() {
    CommandDescriptor currentCommand = myCurrentCommand;
    if (currentCommand != null) return currentCommand.myGroupId;
    if (!myInterruptedCommands.isEmpty()) {
      final CommandDescriptor command = myInterruptedCommands.peek();
      return command != null ? command.myGroupId : null;
    }
    return null;
  }

  @Override
  @Nullable
  public Project getCurrentCommandProject() {
    CommandDescriptor currentCommand = myCurrentCommand;
    return currentCommand != null ? currentCommand.myProject : null;
  }

  @Override
  public void runUndoTransparentAction(@Nonnull Runnable action) {
    if (CommandLog.LOG.isDebugEnabled()) {
      CommandLog.LOG.debug("runUndoTransparentAction: " + action + ", in command = " + (myCurrentCommand != null) + ", in transparent action = " + isUndoTransparentActionInProgress());
    }
    if (myUndoTransparentCount++ == 0) eventPublisher.undoTransparentActionStarted();
    try {
      action.run();
    }
    finally {
      if (myUndoTransparentCount == 1) eventPublisher.beforeUndoTransparentActionFinished();
      if (--myUndoTransparentCount == 0) eventPublisher.undoTransparentActionFinished();
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
    myApplication.assertIsDispatchThread();
    CommandDescriptor currentCommand = myCurrentCommand;
    CommandEvent event = new CommandEvent(this,
                                          currentCommand.myCommand,
                                          currentCommand.myName,
                                          currentCommand.myGroupId,
                                          currentCommand.myProject,
                                          currentCommand.myUndoConfirmationPolicy,
                                          currentCommand.myShouldRecordActionForActiveDocument,
                                          currentCommand.myDocument);
    eventPublisher.commandStarted(event);
  }
}
