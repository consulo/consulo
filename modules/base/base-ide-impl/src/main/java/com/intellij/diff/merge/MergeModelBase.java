/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.diff.merge;

import com.intellij.diff.util.DiffUtil;
import com.intellij.diff.util.LineRange;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.undo.BasicUndoableAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UnexpectedUndoException;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredWriteAction;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public abstract class MergeModelBase<S extends MergeModelBase.State> implements Disposable {
  public static final Logger LOG = Logger.getInstance(MergeModelBase.class);

  @Nullable
  private final Project myProject;
  @Nonnull
  private final Document myDocument;
  @Nullable
  private final UndoManager myUndoManager;

  @Nonnull
  private IntList myStartLines = IntLists.newArrayList();
  @Nonnull
  private IntList myEndLines = IntLists.newArrayList();

  @Nonnull
  private final IntSet myChangesToUpdate = IntSets.newHashSet();
  private int myBulkChangeUpdateDepth;

  private boolean myInsideCommand;

  private boolean myDisposed;

  public MergeModelBase(@Nullable Project project, @Nonnull Document document) {
    myProject = project;
    myDocument = document;
    myUndoManager = myProject != null ? UndoManager.getInstance(myProject) : UndoManager.getGlobalInstance();

    myDocument.addDocumentListener(new MyDocumentListener(), this);
  }

  @Override
  @RequiredUIAccess
  public void dispose() {
    if (myDisposed) return;
    myDisposed = true;

    LOG.assertTrue(myBulkChangeUpdateDepth == 0);

    myStartLines.clear();
    myEndLines.clear();
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  public int getChangesCount() {
    return myStartLines.size();
  }

  public int getLineStart(int index) {
    return myStartLines.get(index);
  }

  public int getLineEnd(int index) {
    return myEndLines.get(index);
  }

  public void setChanges(@Nonnull List<LineRange> changes) {
    myStartLines.clear();
    myEndLines.clear();

    for (LineRange range : changes) {
      myStartLines.add(range.start);
      myEndLines.add(range.end);
    }
  }

  @RequiredUIAccess
  public boolean isInsideCommand() {
    return myInsideCommand;
  }

  private void setLineStart(int index, int line) {
    myStartLines.set(index, line);
  }

  private void setLineEnd(int index, int line) {
    myEndLines.set(index, line);
  }

  //
  // Repaint
  //

  @RequiredUIAccess
  public void invalidateHighlighters(int index) {
    if (myBulkChangeUpdateDepth > 0) {
      myChangesToUpdate.add(index);
    }
    else {
      reinstallHighlighters(index);
    }
  }

  @RequiredUIAccess
  public void enterBulkChangeUpdateBlock() {
    myBulkChangeUpdateDepth++;
  }

  @RequiredUIAccess
  public void exitBulkChangeUpdateBlock() {
    myBulkChangeUpdateDepth--;
    LOG.assertTrue(myBulkChangeUpdateDepth >= 0);

    if (myBulkChangeUpdateDepth == 0) {
      myChangesToUpdate.forEach(index -> {
        reinstallHighlighters(index);
      });
      myChangesToUpdate.clear();
    }
  }

  @RequiredUIAccess
  protected abstract void reinstallHighlighters(int index);

  //
  // Undo
  //

  @Nonnull
  @RequiredUIAccess
  protected abstract S storeChangeState(int index);

  @RequiredUIAccess
  protected void restoreChangeState(@Nonnull S state) {
    setLineStart(state.myIndex, state.myStartLine);
    setLineEnd(state.myIndex, state.myEndLine);
  }

  @Nullable
  @RequiredUIAccess
  protected S processDocumentChange(int index, int oldLine1, int oldLine2, int shift) {
    int line1 = getLineStart(index);
    int line2 = getLineEnd(index);

    DiffUtil.UpdatedLineRange newRange = DiffUtil.updateRangeOnModification(line1, line2, oldLine1, oldLine2, shift);

    // RangeMarker can be updated in a different way
    boolean rangeAffected = newRange.damaged || (oldLine2 >= line1 && oldLine1 <= line2);

    S oldState = rangeAffected ? storeChangeState(index) : null;

    setLineStart(index, newRange.startLine);
    setLineEnd(index, newRange.endLine);

    return oldState;
  }

  private class MyDocumentListener extends DocumentAdapter {
    @Override
    public void beforeDocumentChange(DocumentEvent e) {
      if (isDisposed()) return;
      enterBulkChangeUpdateBlock();

      if (getChangesCount() == 0) return;

      LineRange lineRange = DiffUtil.getAffectedLineRange(e);
      int shift = DiffUtil.countLinesShift(e);

      List<S> corruptedStates = ContainerUtil.newSmartList();
      for (int index = 0; index < getChangesCount(); index++) {
        S oldState = processDocumentChange(index, lineRange.start, lineRange.end, shift);
        if (oldState == null) continue;

        invalidateHighlighters(index);
        if (!isInsideCommand()) corruptedStates.add(oldState);
      }

      if (myUndoManager != null && !corruptedStates.isEmpty()) {
        // document undo is registered inside onDocumentChange, so our undo() will be called after its undo().
        // thus thus we can avoid checks for isUndoInProgress() (to avoid modification of the same TextMergeChange by this listener)
        myUndoManager.undoableActionPerformed(new MyUndoableAction(MergeModelBase.this, corruptedStates, true));
      }
    }

    @Override
    public void documentChanged(DocumentEvent e) {
      if (isDisposed()) return;
      exitBulkChangeUpdateBlock();
    }
  }

  public void executeMergeCommand(@Nullable String commandName,
                                  @Nullable String commandGroupId,
                                  @Nonnull UndoConfirmationPolicy confirmationPolicy,
                                  boolean underBulkUpdate,
                                  @Nullable IntList affectedChanges,
                                  @Nonnull Runnable task) {
    IntList allAffectedChanges = affectedChanges != null ? collectAffectedChanges(affectedChanges) : null;
    DiffUtil.executeWriteCommand(myProject, myDocument, commandName, commandGroupId, confirmationPolicy, underBulkUpdate, () -> {
      LOG.assertTrue(!myInsideCommand);

      // We should restore states after changes in document (by DocumentUndoProvider) to avoid corruption by our onBeforeDocumentChange()
      // Undo actions are performed in backward order, while redo actions are performed in forward order.
      // Thus we should register two UndoableActions.

      myInsideCommand = true;
      enterBulkChangeUpdateBlock();
      registerUndoRedo(true, allAffectedChanges);
      try {
        task.run();
      }
      finally {
        registerUndoRedo(false, allAffectedChanges);
        exitBulkChangeUpdateBlock();
        myInsideCommand = false;
      }
    });
  }

  private void registerUndoRedo(boolean undo, @Nullable IntList affectedChanges) {
    if (myUndoManager == null) return;

    List<S> states;
    if (affectedChanges != null) {
      states = new ArrayList<>(affectedChanges.size());
      affectedChanges.forEach((index) -> {
        states.add(storeChangeState(index));
      });
    }
    else {
      states = new ArrayList<>(getChangesCount());
      for (int index = 0; index < getChangesCount(); index++) {
        states.add(storeChangeState(index));
      }
    }
    myUndoManager.undoableActionPerformed(new MyUndoableAction(this, states, undo));
  }

  private static class MyUndoableAction extends BasicUndoableAction {
    @Nonnull
    private final WeakReference<MergeModelBase> myModelRef;
    @Nonnull
    private final List<? extends State> myStates;
    private final boolean myUndo;

    public MyUndoableAction(@Nonnull MergeModelBase model, @Nonnull List<? extends State> states, boolean undo) {
      super(model.myDocument);
      myModelRef = new WeakReference<>(model);

      myStates = states;
      myUndo = undo;
    }

    @Override
    public final void undo() throws UnexpectedUndoException {
      MergeModelBase model = myModelRef.get();
      if (model != null && myUndo) restoreStates(model);
    }

    @Override
    public final void redo() throws UnexpectedUndoException {
      MergeModelBase model = myModelRef.get();
      if (model != null && !myUndo) restoreStates(model);
    }

    private void restoreStates(@Nonnull MergeModelBase model) {
      if (model.isDisposed()) return;
      if (model.getChangesCount() == 0) return;

      model.enterBulkChangeUpdateBlock();
      try {
        for (State state : myStates) {
          //noinspection unchecked
          model.restoreChangeState(state);
          model.invalidateHighlighters(state.myIndex);
        }
      }
      finally {
        model.exitBulkChangeUpdateBlock();
      }
    }
  }

  //
  // Actions
  //

  @RequiredWriteAction
  public void replaceChange(int index, @Nonnull List<? extends CharSequence> newContent) {
    LOG.assertTrue(isInsideCommand());
    int outputStartLine = getLineStart(index);
    int outputEndLine = getLineEnd(index);

    DiffUtil.applyModification(myDocument, outputStartLine, outputEndLine, newContent);

    if (outputStartLine == outputEndLine) { // onBeforeDocumentChange() should process other cases correctly
      int newOutputEndLine = outputStartLine + newContent.size();
      moveChangesAfterInsertion(index, outputStartLine, newOutputEndLine);
    }
  }

  @RequiredWriteAction
  public void appendChange(int index, @Nonnull List<? extends CharSequence> newContent) {
    LOG.assertTrue(isInsideCommand());
    int outputStartLine = getLineStart(index);
    int outputEndLine = getLineEnd(index);

    DiffUtil.applyModification(myDocument, outputEndLine, outputEndLine, newContent);

    int newOutputEndLine = outputEndLine + newContent.size();
    moveChangesAfterInsertion(index, outputStartLine, newOutputEndLine);
  }

  /*
   * We want to include inserted block into change, so we are updating endLine(BASE).
   *
   * It could break order of changes if there are other changes that starts/ends at this line.
   * So we should check all other changes and shift them if necessary.
   */
  private void moveChangesAfterInsertion(int index,
                                         int newOutputStartLine,
                                         int newOutputEndLine) {
    LOG.assertTrue(isInsideCommand());

    if (getLineStart(index) != newOutputStartLine ||
        getLineEnd(index) != newOutputEndLine) {
      setLineStart(index, newOutputStartLine);
      setLineEnd(index, newOutputEndLine);
      invalidateHighlighters(index);
    }

    boolean beforeChange = true;
    for (int otherIndex = 0; otherIndex < getChangesCount(); otherIndex++) {
      int startLine = getLineStart(otherIndex);
      int endLine = getLineEnd(otherIndex);
      if (endLine < newOutputStartLine) continue;
      if (startLine > newOutputEndLine) break;
      if (index == otherIndex) {
        beforeChange = false;
        continue;
      }

      int newStartLine = beforeChange ? Math.min(startLine, newOutputStartLine) : Math.max(startLine, newOutputEndLine);
      int newEndLine = beforeChange ? Math.min(endLine, newOutputStartLine) : Math.max(endLine, newOutputEndLine);
      if (startLine != newStartLine || endLine != newEndLine) {
        setLineStart(otherIndex, newStartLine);
        setLineEnd(otherIndex, newEndLine);
        invalidateHighlighters(otherIndex);
      }
    }
  }

  /*
   * Nearby changes could be affected as well (ex: by moveChangesAfterInsertion)
   *
   * null means all changes could be affected
   */
  @Nonnull
  @RequiredUIAccess
  private IntList collectAffectedChanges(@Nonnull IntList directChanges) {
    IntList result = IntLists.newArrayList(directChanges.size());

    int directArrayIndex = 0;
    int otherIndex = 0;
    while (directArrayIndex < directChanges.size() && otherIndex < getChangesCount()) {
      int directIndex = directChanges.get(directArrayIndex);

      if (directIndex == otherIndex) {
        result.add(directIndex);
        otherIndex++;
        continue;
      }

      int directStart = getLineStart(directIndex);
      int directEnd = getLineEnd(directIndex);
      int otherStart = getLineStart(otherIndex);
      int otherEnd = getLineEnd(otherIndex);

      if (otherEnd < directStart) {
        otherIndex++;
        continue;
      }
      if (otherStart > directEnd) {
        directArrayIndex++;
        continue;
      }

      result.add(otherIndex);
      otherIndex++;
    }

    LOG.assertTrue(directChanges.size() <= result.size());
    return result;
  }

  //
  // Helpers
  //

  public static class State {
    public final int myIndex;
    public final int myStartLine;
    public final int myEndLine;

    public State(int index, int startLine, int endLine) {
      myIndex = index;
      myStartLine = startLine;
      myEndLine = endLine;
    }
  }
}
