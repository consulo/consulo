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
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2024-10-21
 */
public class CommandDescriptor {
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

    public CommandDescriptor() {
        this(EmptyRunnable.INSTANCE);
    }

    public CommandDescriptor(@Nonnull Runnable command) {
        myCommand = command;
    }

    public CommandDescriptor(
        @Nonnull Runnable myCommand,
        @Nonnull LocalizeValue myName,
        Object myGroupId,
        Project myProject,
        Document myDocument,
        @Nonnull UndoConfirmationPolicy myUndoConfirmationPolicy,
        boolean myShouldRecordActionForActiveDocument
    ) {
        this.myCommand = myCommand;
        this.myProject = myProject;
        this.myName = myName;
        this.myGroupId = myGroupId;
        this.myDocument = myDocument;
        this.myUndoConfirmationPolicy = myUndoConfirmationPolicy;
        this.myShouldRecordActionForActiveDocument = myShouldRecordActionForActiveDocument;
    }

    @Nonnull
    public Runnable getCommand() {
        return myCommand;
    }

    @Nonnull
    public LocalizeValue getName() {
        return myName;
    }

    public Object getGroupId() {
        return myGroupId;
    }

    public Project getProject() {
        return myProject;
    }

    public Document getDocument() {
        return myDocument;
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
