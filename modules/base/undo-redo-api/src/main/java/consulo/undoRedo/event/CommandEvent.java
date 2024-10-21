// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.undoRedo.event;

import consulo.annotation.DeprecationInfo;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;

import java.util.EventObject;

public class CommandEvent extends EventObject {
    private final Runnable myCommand;
    private final Project myProject;
    private final LocalizeValue myCommandName;
    private final Object myCommandGroupId;
    private final UndoConfirmationPolicy myUndoConfirmationPolicy;
    private final boolean myShouldRecordActionForActiveDocument;
    private final Document myDocument;

    public CommandEvent(
        @Nonnull CommandProcessor processor,
        @Nonnull Runnable command,
        Project project,
        @Nonnull UndoConfirmationPolicy undoConfirmationPolicy
    ) {
        this(processor, command, LocalizeValue.empty(), null, project, undoConfirmationPolicy);
    }

    public CommandEvent(
        @Nonnull CommandProcessor processor,
        @Nonnull Runnable command,
        @Nonnull LocalizeValue commandName,
        Object commandGroupId,
        Project project,
        @Nonnull UndoConfirmationPolicy undoConfirmationPolicy
    ) {
        this(processor, command, commandName.get(), commandGroupId, project, undoConfirmationPolicy, true, null);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public CommandEvent(
        @Nonnull CommandProcessor processor,
        @Nonnull Runnable command,
        String commandName,
        Object commandGroupId,
        Project project,
        @Nonnull UndoConfirmationPolicy undoConfirmationPolicy
    ) {
        this(processor, command, LocalizeValue.ofNullable(commandName), commandGroupId, project, undoConfirmationPolicy, true, null);
    }

    public CommandEvent(
        @Nonnull CommandProcessor processor,
        @Nonnull Runnable command,
        @Nonnull LocalizeValue commandName,
        Object commandGroupId,
        Project project,
        @Nonnull UndoConfirmationPolicy undoConfirmationPolicy,
        boolean shouldRecordActionForActiveDocument,
        Document document
    ) {
        super(processor);
        myCommand = command;
        myCommandName = commandName;
        myCommandGroupId = commandGroupId;
        myProject = project;
        myUndoConfirmationPolicy = undoConfirmationPolicy;
        myShouldRecordActionForActiveDocument = shouldRecordActionForActiveDocument;
        myDocument = document;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public CommandEvent(
        @Nonnull CommandProcessor processor,
        @Nonnull Runnable command,
        String commandName,
        Object commandGroupId,
        Project project,
        @Nonnull UndoConfirmationPolicy undoConfirmationPolicy,
        boolean shouldRecordActionForActiveDocument,
        Document document
    ) {
        this(
            processor,
            command,
            LocalizeValue.ofNullable(commandName),
            commandGroupId,
            project,
            undoConfirmationPolicy,
            shouldRecordActionForActiveDocument,
            document
        );
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

    //TODO: rename into getCommandName() after deprecation removal
    public LocalizeValue getCommandNameValue() {
        return myCommandName;
    }

    @Deprecated
    @DeprecationInfo("Use #getCommandNameValue()")
    public String getCommandName() {
        return myCommandName.get();
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