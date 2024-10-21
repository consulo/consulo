/*
 * Copyright 2013-2024 consulo.io
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
package consulo.undoRedo.builder;

import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandDescriptor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;

/**
* @author UNV
* @since 2024-10-21
*/
public class BaseCommandBuilder<THIS extends CommandBuilder> implements CommandBuilder<THIS> {
    @Nonnull
    private final Runnable myCommand;
    @Nonnull
    private LocalizeValue myName = LocalizeValue.empty();
    private Object myGroupId = null;
    private Project myProject = null;
    private Document myDocument = null;
    @Nonnull
    private UndoConfirmationPolicy myUndoConfirmationPolicy = UndoConfirmationPolicy.DEFAULT;
    private boolean myShouldRecordActionForActiveDocument = true;

    public BaseCommandBuilder() {
        this(EmptyRunnable.INSTANCE);
    }

    public BaseCommandBuilder(@Nonnull Runnable command) {
        myCommand = command;
    }

    public BaseCommandBuilder(CommandDescriptor descriptor) {
        this(descriptor.getCommand());
        withName(descriptor.getName());
        withGroupId(descriptor.getGroupId());
        withProject(descriptor.getProject());
        withDocument(descriptor.getDocument());
        withUndoConfirmationPolicy(descriptor.getUndoConfirmationPolicy());
        withShouldRecordActionForActiveDocument(descriptor.isShouldRecordActionForActiveDocument());
    }

    @Override
    public THIS withName(@Nonnull LocalizeValue name) {
        myName = name;
        return self();
    }

    @Override
    public THIS withGroupId(Object groupId) {
        myGroupId = groupId;

        if (groupId instanceof Document docGroupId) {
            myDocument = docGroupId;
        }
        else if (groupId instanceof Ref refGroupId && refGroupId.get() instanceof Document docRefGroupId) {
            myDocument = docRefGroupId;
        }

        return self();
    }

    @Override
    public THIS withProject(Project project) {
        myProject = project;
        return self();
    }

    @Override
    public THIS withDocument(Document document) {
        myDocument = document;
        return self();
    }

    @Override
    public THIS withUndoConfirmationPolicy(@Nonnull UndoConfirmationPolicy undoConfirmationPolicy) {
        myUndoConfirmationPolicy = undoConfirmationPolicy;
        return self();
    }

    /**
     * @param shouldRecordActionForActiveDocument {@code false} if the action is not supposed to be recorded
     *                                            into the currently open document's history.
     *                                            Examples of such actions: Create New File, Change Project Settings etc.
     *                                            Default is {@code true}.
     */
    @Override
    public THIS withShouldRecordActionForActiveDocument(boolean shouldRecordActionForActiveDocument) {
        myShouldRecordActionForActiveDocument = shouldRecordActionForActiveDocument;
        return self();
    }

    @Override
    public CommandDescriptor build() {
        return new CommandDescriptor(
            myCommand,
            myName,
            myGroupId,
            myProject,
            myDocument,
            myUndoConfirmationPolicy,
            myShouldRecordActionForActiveDocument
        );
    }

    @SuppressWarnings("unchecked")
    protected THIS self() {
        return (THIS)this;
    }
}
