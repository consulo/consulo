/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.command.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

public class CoreCommandProcessor extends CommandProcessorEx {
  private static class CommandDescriptor implements CommandToken {
    @Nonnull
    public final Supplier<AsyncResult<Void>> myCommand;
    public final Project myProject;
    public String myName;
    public Object myGroupId;
    public final Document myDocument;
    @Nonnull
    public final UndoConfirmationPolicy myUndoConfirmationPolicy;
    public final boolean myShouldRecordActionForActiveDocument;

    CommandDescriptor(@Nonnull Supplier<AsyncResult<Void>> command,
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

    @Nullable
    @Override
    public Project getProject() {
      return myProject;
    }

    @Override
    public String toString() {
      return "'" + myName + "', group: '" + myGroupId + "'";
    }
  }

  protected CommandDescriptor myCurrentCommand;
  private final Stack<CommandDescriptor> myInterruptedCommands = new Stack<>();
  private final List<CommandListener> myNewListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private int myUndoTransparentCount;

  private final CommandListener eventPublisher;

  public CoreCommandProcessor() {
    MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
    messageBus.connect().subscribe(CommandListener.TOPIC, new CommandListener() {
      @Override
      public void commandStarted(@Nonnull CommandEvent event) {
        for (CommandListener listener : myNewListeners) {
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
        for (CommandListener listener : myNewListeners) {
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
        for (CommandListener listener : myNewListeners) {
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
        for (CommandListener listener : myNewListeners) {
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
        for (CommandListener listener : myNewListeners) {
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
        for (CommandListener listener : myNewListeners) {
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
    eventPublisher = messageBus.syncPublisher(CommandListener.TOPIC);
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
    executeCommandAsync(project, wrap(command), name, groupId, confirmationPolicy, true, document);
  }

  @Override
  public void executeCommand(@Nullable Project project,
                             @Nonnull Runnable command,
                             @Nullable String name,
                             @Nullable Object groupId,
                             @Nonnull UndoConfirmationPolicy confirmationPolicy,
                             boolean shouldRecordCommandForActiveDocument) {
    executeCommandAsync(project, wrap(command), name, groupId, confirmationPolicy, shouldRecordCommandForActiveDocument, null);
  }

  private static Supplier<AsyncResult<Void>> wrap(Runnable runnable) {
    return () -> {
      runnable.run();
      return AsyncResult.resolved();
    };
  }

  @RequiredUIAccess
  @Override
  public void executeCommandAsync(Project project, @Nonnull Supplier<AsyncResult<Void>> runnable, String name, Object groupId) {
    executeCommandAsync(project, runnable, name, groupId, UndoConfirmationPolicy.DEFAULT);
  }

  @RequiredUIAccess
  @Override
  public void executeCommandAsync(Project project, @Nonnull Supplier<AsyncResult<Void>> runnable, String name, Object groupId, Document document) {
    executeCommandAsync(project, runnable, name, groupId, UndoConfirmationPolicy.DEFAULT, document);
  }

  @RequiredUIAccess
  @Override
  public void executeCommandAsync(Project project, @Nonnull final Supplier<AsyncResult<Void>> command, final String name, final Object groupId, @Nonnull UndoConfirmationPolicy confirmationPolicy) {
    executeCommandAsync(project, command, name, groupId, confirmationPolicy, null);
  }

  @RequiredUIAccess
  @Override
  public void executeCommandAsync(Project project,
                                  @Nonnull final Supplier<AsyncResult<Void>> command,
                                  @Nullable String name,
                                  final Object groupId,
                                  @Nonnull UndoConfirmationPolicy confirmationPolicy,
                                  Document document) {
    executeCommandAsync(project, command, name, groupId, confirmationPolicy, true, document);
  }

  @RequiredUIAccess
  @Override
  public void executeCommandAsync(@Nullable Project project,
                                  @Nonnull Supplier<AsyncResult<Void>> command,
                                  @Nullable String name,
                                  @Nullable Object groupId,
                                  @Nonnull UndoConfirmationPolicy confirmationPolicy,
                                  boolean shouldRecordCommandForActiveDocument) {
    executeCommandAsync(project, command, name, groupId, confirmationPolicy, shouldRecordCommandForActiveDocument, null);
  }

  @RequiredUIAccess
  private void executeCommandAsync(@Nullable Project project,
                                   @Nonnull Supplier<AsyncResult<Void>> command,
                                   @Nullable String name,
                                   @Nullable Object groupId,
                                   @Nonnull UndoConfirmationPolicy confirmationPolicy,
                                   boolean shouldRecordCommandForActiveDocument,
                                   @Nullable Document document) {
    UIAccess.assertIsUIThread();

    if (project != null && project.isDisposed()) {
      CommandLog.LOG.error("Project " + project + " already disposed");
      return;
    }

    if (CommandLog.LOG.isDebugEnabled()) {
      CommandLog.LOG.debug("executeCommand: " + command + ", name = " + name + ", groupId = " + groupId);
    }

    if (myCurrentCommand != null) {
      command.get();
      return;
    }

    try {
      UIAccess uiAccess = UIAccess.get();

      CommandDescriptor descriptor = new CommandDescriptor(command, project, name, groupId, confirmationPolicy, shouldRecordCommandForActiveDocument, document);
      
      myCurrentCommand = descriptor;

      fireCommandStarted();

      AsyncResult<Void> result = command.get();

      result.doWhenDone(() -> call(uiAccess, () -> finishCommand(descriptor, null)));

      result.doWhenRejectedWithThrowable((t) -> call(uiAccess, () -> finishCommand(descriptor, t)));
    }
    catch (Throwable th) {
      // in case error not from async result - finish action
      finishCommand(myCurrentCommand, th);
    }
  }

  private static void call(UIAccess uiAccess, @RequiredUIAccess Runnable runnable) {
    if(UIAccess.isUIThread()) {
      runnable.run();
    } else {
      uiAccess.giveAndWait(runnable);
    }
  }

  @Override
  @Nullable
  @RequiredUIAccess
  public CommandToken startCommand(@Nonnull final Project project, @Nls final String name, final Object groupId, @Nonnull final UndoConfirmationPolicy undoConfirmationPolicy) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (project.isDisposed()) return null;

    if (CommandLog.LOG.isDebugEnabled()) {
      CommandLog.LOG.debug("startCommand: name = " + name + ", groupId = " + groupId);
    }

    if (myCurrentCommand != null) {
      return null;
    }

    Document document = groupId instanceof Ref && ((Ref)groupId).get() instanceof Document ? (Document)((Ref)groupId).get() : null;
    myCurrentCommand = new CommandDescriptor(wrap(EmptyRunnable.INSTANCE), project, name, groupId, undoConfirmationPolicy, true, document);
    fireCommandStarted();
    return myCurrentCommand;
  }

  @Override
  @RequiredUIAccess
  public void finishCommand(CommandToken command, final Throwable throwable) {
    UIAccess.assertIsUIThread();

    CommandLog.LOG.assertTrue(myCurrentCommand != null, "no current command in progress");
    fireCommandFinished();
  }

  @RequiredUIAccess
  protected void fireCommandFinished() {
    UIAccess.assertIsUIThread();

    CommandDescriptor currentCommand = myCurrentCommand;
    CommandEvent event = new CommandEvent(this, currentCommand.myCommand, currentCommand.myName, currentCommand.myGroupId, currentCommand.myProject, currentCommand.myUndoConfirmationPolicy,
                                          currentCommand.myShouldRecordActionForActiveDocument, currentCommand.myDocument);
    try {
      eventPublisher.beforeCommandFinished(event);
    }
    finally {
      myCurrentCommand = null;
      eventPublisher.commandFinished(event);
    }
  }

  @Override
  @RequiredUIAccess
  public void enterModal() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    CommandDescriptor currentCommand = myCurrentCommand;
    myInterruptedCommands.push(currentCommand);
    if (currentCommand != null) {
      fireCommandFinished();
    }
  }

  @Override
  @RequiredUIAccess
  public void leaveModal() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    CommandLog.LOG.assertTrue(myCurrentCommand == null, "Command must not run: " + myCurrentCommand);

    myCurrentCommand = myInterruptedCommands.pop();
    if (myCurrentCommand != null) {
      fireCommandStarted();
    }
  }

  @Override
  @RequiredUIAccess
  public void setCurrentCommandName(String name) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    CommandDescriptor currentCommand = myCurrentCommand;
    CommandLog.LOG.assertTrue(currentCommand != null);
    currentCommand.myName = name;
  }

  @Override
  @RequiredUIAccess
  public void setCurrentCommandGroupId(Object groupId) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    CommandDescriptor currentCommand = myCurrentCommand;
    CommandLog.LOG.assertTrue(currentCommand != null);
    currentCommand.myGroupId = groupId;
  }

  @Override
  public boolean hasCurrentCommand() {
    CommandDescriptor currentCommand = myCurrentCommand;
    return currentCommand != null;
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
    myNewListeners.add(listener);
  }

  @Override
  public void addCommandListener(@Nonnull final CommandListener listener, @Nonnull Disposable parentDisposable) {
    addCommandListener(listener);
    Disposer.register(parentDisposable, () -> removeCommandListener(listener));
  }

  @Override
  public void removeCommandListener(@Nonnull CommandListener listener) {
    myNewListeners.remove(listener);
  }

  @Override
  public void runUndoTransparentAction(@Nonnull Runnable action) {
    if (myUndoTransparentCount++ == 0) fireUndoTransparentStarted();
    try {
      action.run();
    }
    finally {
      if (--myUndoTransparentCount == 0) fireUndoTransparentFinished();
    }
  }

  @Override
  public boolean isUndoTransparentActionInProgress() {
    return myUndoTransparentCount > 0;
  }

  @Override
  public void markCurrentCommandAsGlobal(Project project) {
  }


  @Override
  public void addAffectedDocuments(Project project, @Nonnull Document... docs) {
  }

  @Override
  public void addAffectedFiles(Project project, @Nonnull VirtualFile... files) {
  }

  @RequiredUIAccess
  private void fireCommandStarted() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    CommandDescriptor currentCommand = myCurrentCommand;
    CommandEvent event = new CommandEvent(this, currentCommand.myCommand, currentCommand.myName, currentCommand.myGroupId, currentCommand.myProject, currentCommand.myUndoConfirmationPolicy,
                                          currentCommand.myShouldRecordActionForActiveDocument, currentCommand.myDocument);
    eventPublisher.commandStarted(event);
  }

  private void fireUndoTransparentStarted() {
    eventPublisher.undoTransparentActionStarted();
  }

  private void fireUndoTransparentFinished() {
    eventPublisher.undoTransparentActionFinished();
  }
}
