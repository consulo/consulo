/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.application.Application;
import consulo.document.Document;
import consulo.document.DocumentReference;
import consulo.document.impl.DocumentReferenceByDocument;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorStateLevel;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.UndoableAction;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

abstract class UndoRedo {
    protected final UndoManagerImpl myManager;
    protected final FileEditor myEditor;
    protected final UndoableGroup myUndoableGroup;

    //public static void execute(UndoManagerImpl manager, FileEditor editor, boolean isUndo) {
    //  do {
    //    UndoRedo undoOrRedo = isUndo ? new Undo(manager, editor) : new Redo(manager, editor);
    //    undoOrRedo.doExecute();
    //    boolean shouldRepeat = undoOrRedo.isTransparent() && undoOrRedo.hasMoreActions();
    //    if (!shouldRepeat) break;
    //  }
    //  while (true);
    //}
    //
    protected UndoRedo(UndoManagerImpl manager, FileEditor editor) {
        myManager = manager;
        myEditor = editor;
        myUndoableGroup = getLastAction();
    }

    private UndoableGroup getLastAction() {
        return getStackHolder().getLastAction(getDecRefs());
    }

    boolean isTransparent() {
        return myUndoableGroup.isTransparent();
    }

    boolean hasMoreActions() {
        return getStackHolder().canBeUndoneOrRedone(getDecRefs());
    }

    private Set<DocumentReference> getDecRefs() {
        return myEditor == null ? Collections.emptySet() : UndoManagerImpl.getDocumentReferences(myEditor);
    }

    protected abstract UndoRedoStacksHolder getStackHolder();

    protected abstract UndoRedoStacksHolder getReverseStackHolder();

    protected abstract String getActionName();

    protected abstract String getActionName(String commandName);

    protected abstract EditorAndState getBeforeState();

    protected abstract EditorAndState getAfterState();

    protected abstract void performAction();

    protected abstract void setBeforeState(EditorAndState state);

    @RequiredUIAccess
    public boolean execute(boolean drop, boolean isInsideStartFinishGroup) {
        if (!myUndoableGroup.isUndoable()) {
            reportCannotUndo(
                CommonLocalize.cannotUndoErrorContainsNonundoableChangesMessage().get(),
                myUndoableGroup.getAffectedDocuments()
            );
            return false;
        }

        Set<DocumentReference> clashing = getStackHolder().collectClashingActions(myUndoableGroup);
        if (!clashing.isEmpty()) {
            reportCannotUndo(CommonLocalize.cannotUndoErrorOtherAffectedFilesChangedMessage().get(), clashing);
            return false;
        }


        if (!isInsideStartFinishGroup && myUndoableGroup.shouldAskConfirmation(isRedo())) {
            if (!askUser()) {
                return false;
            }
        }
        else {
            if (restore(getBeforeState(), true)) {
                setBeforeState(new EditorAndState(myEditor, myEditor.getState(FileEditorStateLevel.UNDO)));
                return true;
            }
        }

        Collection<VirtualFile> readOnlyFiles = collectReadOnlyAffectedFiles();
        if (!readOnlyFiles.isEmpty()) {
            Project project = myManager.getProject();
            VirtualFile[] files = VfsUtil.toVirtualFileArray(readOnlyFiles);

            if (project == null) {
                return false;
            }

            ReadonlyStatusHandler.OperationStatus operationStatus = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(files);
            if (operationStatus.hasReadonlyFiles()) {
                return false;
            }
        }

        Collection<Document> readOnlyDocuments = collectReadOnlyDocuments();
        if (!readOnlyDocuments.isEmpty()) {
            for (Document document : readOnlyDocuments) {
                document.fireReadOnlyModificationAttempt();
            }
            return false;
        }

        getStackHolder().removeFromStacks(myUndoableGroup);
        if (!drop) {
            getReverseStackHolder().addToStacks(myUndoableGroup);
        }

        performAction();

        restore(getAfterState(), false);

        return true;
    }

    protected abstract boolean isRedo();

    private Collection<Document> collectReadOnlyDocuments() {
        Collection<Document> readOnlyDocs = new ArrayList<>();
        for (UndoableAction action : myUndoableGroup.getActions()) {
            if (action instanceof MentionOnlyUndoableAction) {
                continue;
            }

            DocumentReference[] refs = action.getAffectedDocuments();
            if (refs == null) {
                continue;
            }

            for (DocumentReference ref : refs) {
                if (ref instanceof DocumentReferenceByDocument) {
                    Document doc = ref.getDocument();
                    if (doc != null && !doc.isWritable()) {
                        readOnlyDocs.add(doc);
                    }
                }
            }
        }
        return readOnlyDocs;
    }

    private Collection<VirtualFile> collectReadOnlyAffectedFiles() {
        Collection<VirtualFile> readOnlyFiles = new ArrayList<>();
        for (UndoableAction action : myUndoableGroup.getActions()) {
            if (action instanceof MentionOnlyUndoableAction) {
                continue;
            }

            DocumentReference[] refs = action.getAffectedDocuments();
            if (refs == null) {
                continue;
            }

            for (DocumentReference ref : refs) {
                VirtualFile file = ref.getFile();
                if ((file != null) && file.isValid() && !file.isWritable()) {
                    readOnlyFiles.add(file);
                }
            }
        }
        return readOnlyFiles;
    }

    @RequiredUIAccess
    private void reportCannotUndo(String message, Collection<DocumentReference> problemFiles) {
        if (Application.get().isUnitTestMode()) {
            throw new RuntimeException(
                message + "\n" + StringUtil.join(problemFiles, Object::toString, "\n")
            );
        }
        new CannotUndoReportDialog(myManager.getProject(), message, problemFiles).show();
    }

    @RequiredUIAccess
    private boolean askUser() {
        String actionText = getActionName(myUndoableGroup.getCommandName());

        if (actionText.length() > 80) {
            actionText = actionText.substring(0, 80) + "... ";
        }

        return Messages.showOkCancelDialog(
            myManager.getProject(),
            actionText + "?",
            getActionName(),
            UIUtil.getQuestionIcon()
        ) == Messages.OK;
    }

    private boolean restore(EditorAndState pair, boolean onlyIfDiffers) {
        if (myEditor == null ||
            !myEditor.isValid() || // editor can be invalid if underlying file is deleted during undo (e.g. after undoing scratch file creation)
            pair == null || pair.getEditor() == null) {
            return false;
        }

        // we cannot simply compare editors here because of the following scenario:
        // 1. make changes in editor for file A
        // 2. move caret
        // 3. close editor
        // 4. re-open editor for A via Ctrl-E
        // 5. undo -> position is not affected, because instance created in step 4 is not the same!!!
        if (!myEditor.getClass().equals(pair.getEditor().getClass())) {
            return false;
        }

        // If current editor state isn't equals to remembered state then
        // we have to try to restore previous state. But sometime it's
        // not possible to restore it. For example, it's not possible to
        // restore scroll proportion if editor does not have scrolling any more.
        FileEditorState currentState = myEditor.getState(FileEditorStateLevel.UNDO);
        if (onlyIfDiffers && currentState.equals(pair.getState())) {
            return false;
        }

        myEditor.setState(pair.getState());
        return true;
    }
}
