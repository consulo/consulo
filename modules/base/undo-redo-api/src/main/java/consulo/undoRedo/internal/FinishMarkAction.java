/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.undoRedo.internal;

import consulo.document.Document;
import consulo.document.DocumentReference;
import consulo.document.DocumentReferenceManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.BasicUndoableAction;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.ProjectUndoManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author anna
 * @since 2011-11-08
 */
public class FinishMarkAction extends BasicUndoableAction {
    @Nonnull
    private final StartMarkAction myStartAction;
    private boolean myGlobal = false;
    private String myCommandName;
    private DocumentReference myReference;

    private FinishMarkAction(DocumentReference reference, @Nonnull StartMarkAction action) {
        super(reference);
        myReference = reference;
        myStartAction = action;
    }

    @Override
    public void undo() {
    }

    @Override
    public void redo() {
    }

    @Override
    public boolean isGlobal() {
        return myGlobal;
    }

    public void setGlobal(boolean isGlobal) {
        myStartAction.setGlobal(isGlobal);
        myGlobal = isGlobal;
    }

    public void setCommandName(String commandName) {
        myStartAction.setCommandName(commandName);
        myCommandName = commandName;
    }

    public String getCommandName() {
        return myCommandName;
    }

    public DocumentReference getAffectedDocument() {
        return myReference;
    }

    @RequiredUIAccess
    public static void finish(final Project project, final Document document, @Nullable final StartMarkAction startAction) {
        if (startAction == null) {
            return;
        }
        CommandProcessor.getInstance().newCommand(() -> {
                DocumentReference reference = DocumentReferenceManager.getInstance().create(document);
                ProjectUndoManager.getInstance(project).undoableActionPerformed(new FinishMarkAction(reference, startAction));
                StartMarkAction.markFinished(project);
            })
            .withProject(project)
            .withName(LocalizeValue.localizeTODO("finish"))
            .execute();
    }
}
