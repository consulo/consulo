package com.intellij.mock;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.RequiredUIAccess;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

@Deprecated
public class MockCommandProcessor extends CommandProcessor {
  @Override
  public void executeCommand(@Nonnull Runnable runnable, String name, Object groupId) {
  }

  @Override
  public void executeCommand(Project project, @Nonnull Runnable runnable, String name, Object groupId) {
  }

  @Override
  public void executeCommand(Project project, @Nonnull Runnable runnable, String name, Object groupId, @Nonnull UndoConfirmationPolicy confirmationPolicy) {

  }

  @Override
  public void executeCommand(@Nullable Project project,
                             @Nonnull Runnable command,
                             @Nullable String name,
                             @Nullable Object groupId,
                             @Nonnull UndoConfirmationPolicy confirmationPolicy,
                             boolean shouldRecordCommandForActiveDocument) {
  }

  @RequiredUIAccess
  @Override
  public <T> AsyncResult<T> executeCommandAsync(@Nullable Project project,
                                                @Nonnull Consumer<AsyncResult<T>> command,
                                                @Nullable String name,
                                                @Nullable Object groupId,
                                                @Nonnull UndoConfirmationPolicy confirmationPolicy,
                                                @Nullable Document document) {
    return null;
  }

  @RequiredUIAccess
  @Override
  public <T> AsyncResult<T> executeCommandAsync(@Nullable Project project,
                                                @Nonnull Consumer<AsyncResult<T>> command,
                                                @Nullable String name,
                                                @Nullable Object groupId,
                                                @Nonnull UndoConfirmationPolicy confirmationPolicy,
                                                boolean shouldRecordCommandForActiveDocument) {
    return null;
  }

  @Override
  public void setCurrentCommandName(String name) {
  }

  @Override
  public void setCurrentCommandGroupId(Object groupId) {
  }

  @Override
  public boolean hasCurrentCommand() {
    return false;
  }

  @Override
  public String getCurrentCommandName() {
    return null;
  }

  @Override
  @Nullable
  public Object getCurrentCommandGroupId() {
    return null;
  }

  @Override
  public Project getCurrentCommandProject() {
    return null;
  }

  @Override
  public void addCommandListener(@Nonnull CommandListener listener) {
  }

  @Override
  public void addCommandListener(@Nonnull CommandListener listener, @Nonnull Disposable parentDisposable) {
  }

  @Override
  public void removeCommandListener(@Nonnull CommandListener listener) {
  }

  @Override
  public boolean isUndoTransparentActionInProgress() {
    return false;
  }

  @Override
  public void markCurrentCommandAsGlobal(Project project) {

  }

  @Override
  public void runUndoTransparentAction(@Nonnull Runnable action) {
  }

  @Nonnull
  @Override
  public <T> AsyncResult<T> runUndoTransparentActionAsync(@Nonnull Consumer<AsyncResult<T>> consumer) {
    return null;
  }

  @Override
  public void executeCommand(Project project, @Nonnull Runnable command, String name, Object groupId, @Nonnull UndoConfirmationPolicy confirmationPolicy, Document document) {
  }

  @Override
  public void executeCommand(Project project, @Nonnull Runnable runnable, @Nls String name, Object groupId, @Nullable Document document) {

  }

  @Override
  public void addAffectedDocuments(Project project, @Nonnull Document... docs) {
  }

  @Override
  public void addAffectedFiles(Project project, @Nonnull VirtualFile... files) {
  }
}
