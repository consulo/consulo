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
package consulo.undoRedo;

import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2024-10-21
 */
public class CommandDescriptor {
    @Nonnull
    private final Runnable myCommand;
    private Project myProject = null;
    private LocalizeValue myName = LocalizeValue.empty();
    private Object myGroupId = null;
    private Document myDocument = null;
    @Nonnull
    private UndoConfirmationPolicy myUndoConfirmationPolicy = UndoConfirmationPolicy.DEFAULT;
    private boolean myShouldRecordActionForActiveDocument = true;

    private boolean myLocked = false;

    public CommandDescriptor() {
        this(EmptyRunnable.INSTANCE);
    }

    public CommandDescriptor(@Nonnull Runnable command) {
        myCommand = command;
    }

    public CommandDescriptor project(@Nonnull Project project) {
        ensureNonLocked();
        myProject = project;
        return this;
    }

    public CommandDescriptor document(@Nonnull Document document) {
        ensureNonLocked();
        myDocument = document;
        return this;
    }

    public CommandDescriptor name(@Nonnull LocalizeValue name) {
        myName = name;
        return this;
    }

    public CommandDescriptor groupId(@Nonnull Object groupId) {
        myGroupId = groupId;

        if (groupId instanceof Document docGroupId) {
            myDocument = docGroupId;
        }
        else if (groupId instanceof Ref refGroupId && refGroupId.get() instanceof Document docRefGroupId) {
            myDocument = docRefGroupId;
        }

        return this;
    }

    public CommandDescriptor undoConfirmationPolicy(@Nonnull UndoConfirmationPolicy undoConfirmationPolicy) {
        ensureNonLocked();
        myUndoConfirmationPolicy = undoConfirmationPolicy;
        return this;
    }

    /**
     * @param shouldRecordActionForActiveDocument {@code false} if the action is not supposed to be recorded
     *                                            into the currently open document's history.
     *                                            Examples of such actions: Create New File, Change Project Settings etc.
     *                                            Default is {@code true}.
     */
    public CommandDescriptor shouldRecordActionForActiveDocument(boolean shouldRecordActionForActiveDocument) {
        ensureNonLocked();
        myShouldRecordActionForActiveDocument = shouldRecordActionForActiveDocument;
        return this;
    }

    public void lock() {
        myLocked = true;
    }

    private void ensureNonLocked() {
        if (myLocked) {
            throw new IllegalStateException("Attempting to modify after locking");
        }
    }

    @Nonnull
    public Runnable getCommand() {
        return myCommand;
    }

    public Project getProject() {
        return myProject;
    }

    public Document getDocument() {
        return myDocument;
    }

    public LocalizeValue getName() {
        return myName;
    }

    public Object getGroupId() {
        return myGroupId;
    }

    public UndoConfirmationPolicy getUndoConfirmationPolicy() {
        return myUndoConfirmationPolicy;
    }

    public boolean isShouldRecordActionForActiveDocument() {
        return myShouldRecordActionForActiveDocument;
    }

    @Override
    public String toString() {
        return "'" + myName + "', group: '" + myGroupId + "'";
    }
}
