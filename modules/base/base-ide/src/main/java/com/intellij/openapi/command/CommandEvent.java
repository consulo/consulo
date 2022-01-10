// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.command;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.EventObject;

public class CommandEvent extends EventObject {
  private final Runnable myCommand;
  private final Project myProject;
  private final String myCommandName;
  private final Object myCommandGroupId;
  private final UndoConfirmationPolicy myUndoConfirmationPolicy;
  private final boolean myShouldRecordActionForActiveDocument;
  private final Document myDocument;

  public CommandEvent(@Nonnull CommandProcessor processor, @Nonnull Runnable command, Project project, @Nonnull UndoConfirmationPolicy undoConfirmationPolicy) {
    this(processor, command, null, null, project, undoConfirmationPolicy);
  }

  public CommandEvent(@Nonnull CommandProcessor processor,
                      @Nonnull Runnable command,
                      String commandName,
                      Object commandGroupId,
                      Project project,
                      @Nonnull UndoConfirmationPolicy undoConfirmationPolicy) {
    this(processor, command, commandName, commandGroupId, project, undoConfirmationPolicy, true, null);
  }

  public CommandEvent(@Nonnull CommandProcessor processor,
                      @Nonnull Runnable command,
                      String commandName,
                      Object commandGroupId,
                      Project project,
                      @Nonnull UndoConfirmationPolicy undoConfirmationPolicy,
                      boolean shouldRecordActionForActiveDocument,
                      Document document) {
    super(processor);
    myCommand = command;
    myCommandName = commandName;
    myCommandGroupId = commandGroupId;
    myProject = project;
    myUndoConfirmationPolicy = undoConfirmationPolicy;
    myShouldRecordActionForActiveDocument = shouldRecordActionForActiveDocument;
    myDocument = document;
  }

  @Nonnull
  public CommandProcessor getCommandProcessor() {
    return (CommandProcessor)getSource();
  }

  @Nonnull
  public Runnable getCommand() {
    return myCommand;
  }

  public Project getProject() {
    return myProject;
  }

  public String getCommandName() {
    return myCommandName;
  }

  public Object getCommandGroupId() {
    return myCommandGroupId;
  }

  @Nonnull
  public UndoConfirmationPolicy getUndoConfirmationPolicy() {
    return myUndoConfirmationPolicy;
  }

  public boolean shouldRecordActionForOriginalDocument() {
    return myShouldRecordActionForActiveDocument;
  }

  @Nullable
  public Document getDocument() {
    return myDocument;
  }
}