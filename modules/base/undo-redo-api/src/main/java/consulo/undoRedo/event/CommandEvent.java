// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.undoRedo.event;

import consulo.annotation.DeprecationInfo;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;

import org.jspecify.annotations.Nullable;

import java.util.EventObject;

public class CommandEvent extends EventObject {
    private final Runnable myCommand;
    private final Project myProject;
    private final LocalizeValue myCommandName;
    private final @Nullable Object myCommandGroupId;
    private final UndoConfirmationPolicy myUndoConfirmationPolicy;
    private final boolean myShouldRecordActionForActiveDocument;
    private final @Nullable Document myDocument;

    public CommandEvent(
        CommandProcessor processor,
        Runnable command,
        Project project,
        UndoConfirmationPolicy undoConfirmationPolicy
    ) {
        this(processor, command, LocalizeValue.empty(), null, project, undoConfirmationPolicy);
    }

    public CommandEvent(
        CommandProcessor processor,
        Runnable command,
        LocalizeValue commandName,
        @Nullable Object commandGroupId,
        Project project,
        UndoConfirmationPolicy undoConfirmationPolicy
    ) {
        this(processor, command, commandName.get(), commandGroupId, project, undoConfirmationPolicy, true, null);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public CommandEvent(
        CommandProcessor processor,
        Runnable command,
        String commandName,
        Object commandGroupId,
        Project project,
        UndoConfirmationPolicy undoConfirmationPolicy
    ) {
        this(processor, command, LocalizeValue.ofNullable(commandName), commandGroupId, project, undoConfirmationPolicy, true, null);
    }

    public CommandEvent(
        CommandProcessor processor,
        Runnable command,
        LocalizeValue commandName,
        @Nullable Object commandGroupId,
        Project project,
        UndoConfirmationPolicy undoConfirmationPolicy,
        boolean shouldRecordActionForActiveDocument,
        @Nullable Document document
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
        CommandProcessor processor,
        Runnable command,
        String commandName,
        @Nullable Object commandGroupId,
        Project project,
        UndoConfirmationPolicy undoConfirmationPolicy,
        boolean shouldRecordActionForActiveDocument,
        @Nullable Document document
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

    public CommandProcessor getCommandProcessor() {
        return (CommandProcessor)getSource();
    }

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

    public @Nullable Object getCommandGroupId() {
        return myCommandGroupId;
    }

    public UndoConfirmationPolicy getUndoConfirmationPolicy() {
        return myUndoConfirmationPolicy;
    }

    public boolean shouldRecordActionForOriginalDocument() {
        return myShouldRecordActionForActiveDocument;
    }

    public @Nullable Document getDocument() {
        return myDocument;
    }
}