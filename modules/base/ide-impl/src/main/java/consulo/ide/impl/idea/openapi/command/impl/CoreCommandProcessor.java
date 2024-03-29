// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.application.Application;
import consulo.component.messagebus.MessageBus;
import consulo.document.Document;
import consulo.ide.impl.idea.openapi.command.CommandProcessorEx;
import consulo.ide.impl.idea.openapi.command.CommandToken;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.undoRedo.event.CommandEvent;
import consulo.undoRedo.event.CommandListener;
import consulo.util.collection.Lists;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.util.List;
import java.util.Stack;

public class CoreCommandProcessor extends CommandProcessorEx {
  private static class CommandDescriptor implements CommandToken {
    @Nonnull
    public final Runnable myCommand;
    public final Project myProject;
    public String myName;
    public Object myGroupId;
    public final Document myDocument;
    @Nonnull
    public final UndoConfirmationPolicy myUndoConfirmationPolicy;
    public final boolean myShouldRecordActionForActiveDocument;

    CommandDescriptor(@Nonnull Runnable command,
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
  }

  @Nonnull
  private final Application myApplication;

  protected CommandDescriptor myCurrentCommand;
  private final Stack<CommandDescriptor> myInterruptedCommands = new Stack<>();
  private final List<CommandListener> myListeners = Lists.newLockFreeCopyOnWriteList();
  private int myUndoTransparentCount;

  private final CommandListener eventPublisher;

  public CoreCommandProcessor(@Nonnull Application application) {
    myApplication = application;
    MessageBus messageBus = application.getMessageBus();
    messageBus.connect().subscribe(CommandListener.class, new CommandListener() {
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
    });

    // will, command events occurred quite often, let's cache publisher
    eventPublisher = messageBus.syncPublisher(CommandListener.class);
  }

  @Override
  public void executeCommand(@Nonnull Runnable runnable, String name, Object groupId) {
    executeCommand(null, runnable, name, groupId);
  }

  @Override
  public void executeCommand(Project project, @Nonnull Runnable runnable, String name, Object groupId) {
    executeCommand(project, runnable, name, groupId, UndoConfirmationPolicy.DEFAULT);
  }

  @Override
  public void executeCommand(Project project, @Nonnull Runnable runnable, String name, Object groupId, Document document) {
    executeCommand(project, runnable, name, groupId, UndoConfirmationPolicy.DEFAULT, document);
  }

  @Override
  public void executeCommand(Project project, @Nonnull final Runnable command, final String name, final Object groupId, @Nonnull UndoConfirmationPolicy confirmationPolicy) {
    executeCommand(project, command, name, groupId, confirmationPolicy, null);
  }

  @Override
  public void executeCommand(Project project, @Nonnull final Runnable command, final String name, final Object groupId, @Nonnull UndoConfirmationPolicy confirmationPolicy, Document document) {
    executeCommand(project, command, name, groupId, confirmationPolicy, true, document);
  }

  @Override
  public void executeCommand(@Nullable Project project,
                             @Nonnull Runnable command,
                             @Nullable String name,
                             @Nullable Object groupId,
                             @Nonnull UndoConfirmationPolicy confirmationPolicy,
                             boolean shouldRecordCommandForActiveDocument) {
    executeCommand(project, command, name, groupId, confirmationPolicy, shouldRecordCommandForActiveDocument, null);
  }

  @RequiredUIAccess
  private void executeCommand(@Nullable Project project,
                              @Nonnull Runnable command,
                              @Nullable String name,
                              @Nullable Object groupId,
                              @Nonnull UndoConfirmationPolicy confirmationPolicy,
                              boolean shouldRecordCommandForActiveDocument,
                              @Nullable Document document) {
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
      myCurrentCommand = new CommandDescriptor(command, project, name, groupId, confirmationPolicy, shouldRecordCommandForActiveDocument, document);
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
  public CommandToken startCommand(@Nullable final Project project, @Nls final String name, @Nullable final Object groupId, @Nonnull final UndoConfirmationPolicy undoConfirmationPolicy) {
    myApplication.assertIsDispatchThread();
    if (project != null && project.isDisposed()) return null;

    if (CommandLog.LOG.isDebugEnabled()) {
      CommandLog.LOG.debug("startCommand: name = " + name + ", groupId = " + groupId);
    }

    if (myCurrentCommand != null) {
      return null;
    }

    Document document = groupId instanceof Document ? (Document)groupId : (groupId instanceof Ref && ((Ref)groupId).get() instanceof Document ? (Document)((Ref)groupId).get() : null);
    myCurrentCommand = new CommandDescriptor(EmptyRunnable.INSTANCE, project, name, groupId, undoConfirmationPolicy, true, document);
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
    CommandEvent event = new CommandEvent(this, currentCommand.myCommand, currentCommand.myName, currentCommand.myGroupId, currentCommand.myProject, currentCommand.myUndoConfirmationPolicy,
                                          currentCommand.myShouldRecordActionForActiveDocument, currentCommand.myDocument);
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
  public void addCommandListener(@Nonnull CommandListener listener) {
    myListeners.add(listener);
  }

  @Override
  public void removeCommandListener(@Nonnull CommandListener listener) {
    myListeners.remove(listener);
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
    CommandEvent event = new CommandEvent(this, currentCommand.myCommand, currentCommand.myName, currentCommand.myGroupId, currentCommand.myProject, currentCommand.myUndoConfirmationPolicy,
                                          currentCommand.myShouldRecordActionForActiveDocument, currentCommand.myDocument);
    eventPublisher.commandStarted(event);
  }
}
