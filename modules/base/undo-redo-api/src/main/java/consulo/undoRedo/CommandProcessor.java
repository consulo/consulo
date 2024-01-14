// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.undoRedo;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.event.CommandListener;
import consulo.util.lang.EmptyRunnable;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A class for defining 'command' scopes. Every undoable change should be executed as part of a command. Commands can nest, in such a case
 * only the outer-most command is taken into account. Commands with the same 'group id' are merged for undo/redo purposes. 'Transparent'
 * actions (commands) are similar to usual commands but don't create a separate undo/redo step - they are undone/redone together with a
 * 'adjacent' non-transparent commands.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CommandProcessor {
  @Nonnull
  public static CommandProcessor getInstance() {
    return Application.get().getInstance(CommandProcessor.class);
  }

  /**
   * @deprecated use {@link #executeCommand(Project, Runnable, String, Object)}
   */
  @Deprecated
  default void executeCommand(@Nonnull CommandRunnable runnable, String name, Object groupId) {
    executeCommand(null, runnable, name, groupId);
  }

  default void executeCommand(Project project, @Nonnull CommandRunnable runnable, String name, Object groupId) {
    executeCommand(project, runnable, name, groupId, UndoConfirmationPolicy.DEFAULT);
  }

  default void executeCommand(Project project, @Nonnull CommandRunnable runnable, String name, Object groupId, Document document) {
    executeCommand(project, runnable, name, groupId, UndoConfirmationPolicy.DEFAULT, document);
  }

  default void executeCommand(Project project,
                              @Nonnull final CommandRunnable command,
                              final String name,
                              final Object groupId,
                              @Nonnull UndoConfirmationPolicy confirmationPolicy) {
    executeCommand(project, command, name, groupId, confirmationPolicy, null);
  }

  default void executeCommand(Project project,
                              @Nonnull final CommandRunnable command,
                              final String name,
                              final Object groupId,
                              @Nonnull UndoConfirmationPolicy confirmationPolicy,
                              Document document) {
    executeCommand(CommandInfo.newBuilder()
                              .withProject(project)
                              .withName(name)
                              .withGroupId(groupId)
                              .withUndoConfirmationPolicy(confirmationPolicy)
                              .withDocument(document)
                              .build(),
                   command);
  }

  default void executeCommand(@Nullable Project project,
                              @Nonnull CommandRunnable command,
                              @Nullable String name,
                              @Nullable Object groupId,
                              @Nonnull UndoConfirmationPolicy confirmationPolicy,
                              boolean shouldRecordCommandForActiveDocument) {
    executeCommand(CommandInfo.newBuilder()
                              .withProject(project)
                              .withName(name)
                              .withGroupId(groupId)
                              .withUndoConfirmationPolicy(confirmationPolicy)
                              .withShouldRecordCommandForActiveDocument(shouldRecordCommandForActiveDocument)
                              .build(),
                   command);
  }


  void executeCommand(CommandInfo commandInfo, CommandRunnable command);

  @Nonnull
  @RequiredUIAccess
  <V> CompletableFuture<V> executeCommandAsync(@Nonnull CommandInfo commandInfo,
                                               @Nonnull CommandRunnableAsync<V> command);

  void setCurrentCommandName(@Nullable String name);

  void setCurrentCommandGroupId(@Nullable Object groupId);

  @Nullable
  @Deprecated
  @DeprecationInfo("Use #hasCurrentCommand()")
  default Runnable getCurrentCommand() {
    return hasCurrentCommand() ? EmptyRunnable.getInstance() : null;
  }

  boolean hasCurrentCommand();

  @Nullable
  String getCurrentCommandName();

  @Nullable
  Object getCurrentCommandGroupId();

  @Nullable
  Project getCurrentCommandProject();

  /**
   * Defines a scope which contains undoable actions, for which there won't be a separate undo/redo step - they will be undone/redone along
   * with 'adjacent' command.
   */
  void runUndoTransparentAction(@Nonnull Runnable action);

  /**
   * @see #runUndoTransparentAction(Runnable)
   */
  boolean isUndoTransparentActionInProgress();

  void markCurrentCommandAsGlobal(@Nullable Project project);

  void addAffectedDocuments(@Nullable Project project, @Nonnull Document... docs);

  void addAffectedFiles(@Nullable Project project, @Nonnull VirtualFile... files);

  /**
   * @deprecated use {@link CommandListener#class}
   */
  @Deprecated
  default void addCommandListener(@Nonnull CommandListener listener, @Nonnull Disposable parentDisposable) {
    Application.get().getMessageBus().connect(parentDisposable).subscribe(CommandListener.class, listener);
  }
}
