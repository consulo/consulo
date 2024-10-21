// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.undoRedo;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.project.Project;
import consulo.undoRedo.event.CommandListener;
import consulo.util.lang.EmptyRunnable;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A class for defining 'command' scopes. Every undoable change should be executed as part of a command. Commands can nest, in such a case
 * only the outer-most command is taken into account. Commands with the same 'group id' are merged for undo/redo purposes. 'Transparent'
 * actions (commands) are similar to usual commands but don't create a separate undo/redo step - they are undone/redone together with a
 * 'adjacent' non-transparent commands.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class CommandProcessor {
    @Nonnull
    public static CommandProcessor getInstance() {
        return Application.get().getInstance(CommandProcessor.class);
    }

    /**
     * @deprecated use {@link #executeCommand(Project, Runnable, String, Object)}
     */
    @Deprecated
    public abstract void executeCommand(@Nonnull Runnable runnable, @Nullable String name, @Nullable Object groupId);

    public abstract void executeCommand(
        @Nullable Project project,
        @Nonnull Runnable runnable,
        @Nullable String name,
        @Nullable Object groupId
    );

    public abstract void executeCommand(
        @Nullable Project project,
        @Nonnull Runnable runnable,
        @Nullable String name,
        @Nullable Object groupId,
        @Nullable Document document
    );

    public abstract void executeCommand(
        @Nullable Project project,
        @Nonnull Runnable runnable,
        @Nullable String name,
        @Nullable Object groupId,
        @Nonnull UndoConfirmationPolicy confirmationPolicy
    );

    public abstract void executeCommand(
        @Nullable Project project,
        @Nonnull Runnable command,
        @Nullable String name,
        @Nullable Object groupId,
        @Nonnull UndoConfirmationPolicy confirmationPolicy,
        @Nullable Document document
    );

    /**
     * @param shouldRecordCommandForActiveDocument {@code false} if the action is not supposed to be recorded into the currently open document's history.
     *                                             Examples of such actions: Create New File, Change Project Settings etc.
     *                                             Default is {@code true}.
     */
    public abstract void executeCommand(
        @Nullable Project project,
        @Nonnull Runnable command,
        @Nullable String name,
        @Nullable Object groupId,
        @Nonnull UndoConfirmationPolicy confirmationPolicy,
        boolean shouldRecordCommandForActiveDocument
    );

    public abstract void setCurrentCommandName(@Nullable String name);

    public abstract void setCurrentCommandGroupId(@Nullable Object groupId);

    @Nullable
    @Deprecated
    @DeprecationInfo("Use #hasCurrentCommand()")
    public final Runnable getCurrentCommand() {
        return hasCurrentCommand() ? EmptyRunnable.getInstance() : null;
    }

    public abstract boolean hasCurrentCommand();

    @Nullable
    public abstract String getCurrentCommandName();

    @Nullable
    public abstract Object getCurrentCommandGroupId();

    @Nullable
    public abstract Project getCurrentCommandProject();

    /**
     * Defines a scope which contains undoable actions, for which there won't be a separate undo/redo step - they will be undone/redone along
     * with 'adjacent' command.
     */
    public abstract void runUndoTransparentAction(@Nonnull Runnable action);

    /**
     * @see #runUndoTransparentAction(Runnable)
     */
    public abstract boolean isUndoTransparentActionInProgress();

    public abstract void markCurrentCommandAsGlobal(@Nullable Project project);

    public abstract void addAffectedDocuments(@Nullable Project project, @Nonnull Document... docs);

    public abstract void addAffectedFiles(@Nullable Project project, @Nonnull VirtualFile... files);

    /**
     * @deprecated use {@link CommandListener#class}
     */
    @Deprecated
    public abstract void addCommandListener(@Nonnull CommandListener listener);

    /**
     * @deprecated use {@link CommandListener#class}
     */
    @Deprecated
    public void addCommandListener(@Nonnull CommandListener listener, @Nonnull Disposable parentDisposable) {
        ApplicationManager.getApplication().getMessageBus().connect(parentDisposable).subscribe(CommandListener.class, listener);
    }

    /**
     * @deprecated use {@link CommandListener#class}
     */
    @Deprecated
    public abstract void removeCommandListener(@Nonnull CommandListener listener);
}
