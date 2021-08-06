// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.editor.impl;

import com.intellij.diagnostic.Dumpable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.PrioritizedDocumentListener;
import com.intellij.openapi.editor.impl.EditorDocumentPriorities;
import com.intellij.openapi.editor.impl.InlineInlayImpl;
import com.intellij.openapi.editor.impl.RangeMarkerTree;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EmptyClipboardOwner;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.*;

/**
 * Common part from desktop caret model
 */
public abstract class CodeEditorCaretModelBase<CARET extends CodeEditorCaretBase> implements CaretModel, PrioritizedDocumentListener, Disposable, Dumpable, InlayModel.Listener {
  private final CodeEditorBase myEditor;

  private final EventDispatcher<CaretListener> myCaretListeners = EventDispatcher.create(CaretListener.class);
  private final EventDispatcher<CaretActionListener> myCaretActionListeners = EventDispatcher.create(CaretActionListener.class);

  private TextAttributes myTextAttributes;

  boolean myIsInUpdate;

  final RangeMarkerTree<CodeEditorCaretBase.PositionMarker> myPositionMarkerTree;
  final RangeMarkerTree<CodeEditorCaretBase.SelectionMarker> mySelectionMarkerTree;

  private final LinkedList<CARET> myCarets = new LinkedList<>();
  @Nonnull
  private volatile CARET myPrimaryCaret;
  private final ThreadLocal<CARET> myCurrentCaret = new ThreadLocal<>(); // active caret in the context of 'runForEachCaret' call
  private boolean myPerformCaretMergingAfterCurrentOperation;
  private boolean myVisualPositionUpdateScheduled;
  private boolean myEditorSizeValidationScheduled;

  int myDocumentUpdateCounter;

  public CodeEditorCaretModelBase(@Nonnull CodeEditorBase editor) {
    myEditor = editor;
    myEditor.addPropertyChangeListener(evt -> {
      if (EditorEx.PROP_COLUMN_MODE.equals(evt.getPropertyName()) && !myEditor.isColumnMode()) {
        for (CARET caret : myCarets) {
          caret.resetVirtualSelection();
        }
      }
    }, this);

    myPositionMarkerTree = new RangeMarkerTree<>(myEditor.getDocument());
    mySelectionMarkerTree = new RangeMarkerTree<>(myEditor.getDocument());
    myPrimaryCaret = createCaret(myEditor, this);
    myCarets.add(myPrimaryCaret);
  }

  @Nonnull
  protected abstract CARET createCaret(CodeEditorBase editor, CodeEditorCaretModelBase<CARET> model);

  public void onBulkDocumentUpdateStarted() {
  }

  public void onBulkDocumentUpdateFinished() {
    doWithCaretMerging(() -> {
    }); // do caret merging if it's not scheduled for later
  }

  @Override
  public void documentChanged(@Nonnull final DocumentEvent e) {
    myIsInUpdate = false;
    myDocumentUpdateCounter++;
    if (!myEditor.getDocument().isInBulkUpdate()) {
      doWithCaretMerging(() -> {
      }); // do caret merging if it's not scheduled for later
      if (myVisualPositionUpdateScheduled) updateVisualPosition();
    }
  }

  @Override
  public void beforeDocumentChange(@Nonnull DocumentEvent e) {
    if (!myEditor.getDocument().isInBulkUpdate() && e.isWholeTextReplaced()) {
      for (CARET caret : myCarets) {
        caret.updateCachedStateIfNeeded(); // logical position will be needed to restore caret position via diff
      }
    }
    myIsInUpdate = true;
    myVisualPositionUpdateScheduled = false;
  }

  @Override
  public int getPriority() {
    return EditorDocumentPriorities.CARET_MODEL;
  }

  @Override
  public void dispose() {
    for (CARET caret : myCarets) {
      Disposer.dispose(caret);
    }
    mySelectionMarkerTree.dispose(myEditor.getDocument());
    myPositionMarkerTree.dispose(myEditor.getDocument());
  }

  public void updateVisualPosition() {
    for (CARET caret : myCarets) {
      caret.updateVisualPosition();
    }
  }

  @Override
  public void moveCaretRelatively(final int columnShift, final int lineShift, final boolean withSelection, final boolean blockSelection, final boolean scrollToCaret) {
    getCurrentCaret().moveCaretRelatively(columnShift, lineShift, withSelection, scrollToCaret);
  }

  @Override
  public void moveToLogicalPosition(@Nonnull LogicalPosition pos) {
    getCurrentCaret().moveToLogicalPosition(pos);
  }

  @Override
  public void moveToVisualPosition(@Nonnull VisualPosition pos) {
    getCurrentCaret().moveToVisualPosition(pos);
  }

  @Override
  public void moveToOffset(int offset) {
    getCurrentCaret().moveToOffset(offset);
  }

  @Override
  public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {
    getCurrentCaret().moveToOffset(offset, locateBeforeSoftWrap);
  }

  @Override
  public boolean isUpToDate() {
    return getCurrentCaret().isUpToDate();
  }

  @Nonnull
  @Override
  public LogicalPosition getLogicalPosition() {
    return getCurrentCaret().getLogicalPosition();
  }

  @Nonnull
  @Override
  public VisualPosition getVisualPosition() {
    return getCurrentCaret().getVisualPosition();
  }

  @Override
  public int getOffset() {
    return getCurrentCaret().getOffset();
  }

  @Override
  public int getVisualLineStart() {
    return getCurrentCaret().getVisualLineStart();
  }

  @Override
  public int getVisualLineEnd() {
    return getCurrentCaret().getVisualLineEnd();
  }

  public int getWordAtCaretStart() {
    return getCurrentCaret().getWordAtCaretStart();
  }

  public int getWordAtCaretEnd() {
    return getCurrentCaret().getWordAtCaretEnd();
  }

  @Override
  public void addCaretListener(@Nonnull final CaretListener listener) {
    myCaretListeners.addListener(listener);
  }

  @Override
  public void removeCaretListener(@Nonnull CaretListener listener) {
    myCaretListeners.removeListener(listener);
  }

  @Override
  @Nonnull
  public TextAttributes getTextAttributes() {
    TextAttributes textAttributes = myTextAttributes;
    if (textAttributes == null) {
      myTextAttributes = textAttributes = new TextAttributes();
      if (myEditor.getSettings().isCaretRowShown()) {
        textAttributes.setBackgroundColor(myEditor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR));
      }
    }

    return textAttributes;
  }

  public void reinitSettings() {
    myTextAttributes = null;
  }

  @Override
  public boolean supportsMultipleCarets() {
    return true;
  }

  @Override
  @Nonnull
  public CodeEditorCaretBase getCurrentCaret() {
    CodeEditorCaretBase currentCaret = myCurrentCaret.get();
    return ApplicationManager.getApplication().isDispatchThread() && currentCaret != null ? currentCaret : getPrimaryCaret();
  }

  @Override
  @Nonnull
  public CARET getPrimaryCaret() {
    return myPrimaryCaret;
  }

  @Override
  public int getCaretCount() {
    synchronized (myCarets) {
      return myCarets.size();
    }
  }

  @Override
  @Nonnull
  public List<Caret> getAllCarets() {
    List<Caret> carets;
    synchronized (myCarets) {
      carets = new ArrayList<>(myCarets);
    }
    Collections.sort(carets, CARET_POSITION_COMPARATOR);
    return carets;
  }

  @Nullable
  @Override
  public Caret getCaretAt(@Nonnull VisualPosition pos) {
    synchronized (myCarets) {
      for (CodeEditorCaretBase caret : myCarets) {
        if (caret.getVisualPosition().equals(pos)) {
          return caret;
        }
      }
      return null;
    }
  }

  @Nullable
  @Override
  public Caret addCaret(@Nonnull VisualPosition pos) {
    return addCaret(pos, true);
  }

  @Nullable
  @Override
  public Caret addCaret(@Nonnull VisualPosition pos, boolean makePrimary) {
    CodeEditorBase.assertIsDispatchThread();
    CARET caret = createCaret(myEditor, this);
    caret.doMoveToVisualPosition(pos, false);
    if (addCaret(caret, makePrimary)) {
      return caret;
    }
    Disposer.dispose(caret);
    return null;
  }

  boolean addCaret(@Nonnull CARET caretToAdd, boolean makePrimary) {
    CodeEditorBase.assertIsDispatchThread();
    for (CARET caret : myCarets) {
      if (caretsOverlap(caret, caretToAdd)) {
        return false;
      }
    }
    synchronized (myCarets) {
      if (makePrimary) {
        myCarets.addLast(caretToAdd);
        myPrimaryCaret = caretToAdd;
      }
      else {
        myCarets.addFirst(caretToAdd);
      }
    }
    fireCaretAdded(caretToAdd);
    return true;
  }

  @Override
  public boolean removeCaret(@Nonnull Caret caret) {
    CodeEditorBase.assertIsDispatchThread();
    if (myCarets.size() <= 1 || !(caret instanceof CodeEditorCaretBase)) {
      return false;
    }
    synchronized (myCarets) {
      if (!myCarets.remove(caret)) {
        return false;
      }
      myPrimaryCaret = myCarets.getLast();
    }
    fireCaretRemoved(caret);
    Disposer.dispose(caret);
    return true;
  }

  @Override
  public void removeSecondaryCarets() {
    CodeEditorBase.assertIsDispatchThread();
    ListIterator<CARET> caretIterator = myCarets.listIterator(myCarets.size() - 1);
    while (caretIterator.hasPrevious()) {
      CARET caret = caretIterator.previous();
      synchronized (myCarets) {
        caretIterator.remove();
      }
      fireCaretRemoved(caret);
      Disposer.dispose(caret);
    }
  }

  @Override
  public void runForEachCaret(@Nonnull final CaretAction action) {
    runForEachCaret(action, false);
  }

  @Override
  public void runForEachCaret(@Nonnull final CaretAction action, final boolean reverseOrder) {
    CodeEditorBase.assertIsDispatchThread();
    if (myCurrentCaret.get() != null) {
      throw new IllegalStateException("Recursive runForEachCaret invocations are not allowed");
    }
    myCaretActionListeners.getMulticaster().beforeAllCaretsAction();
    doWithCaretMerging(() -> {
      try {
        List<Caret> sortedCarets = getAllCarets();
        if (reverseOrder) {
          Collections.reverse(sortedCarets);
        }
        for (Caret caret : sortedCarets) {
          myCurrentCaret.set((CARET)caret);
          action.perform(caret);
        }
      }
      finally {
        myCurrentCaret.set(null);
      }
    });
    myCaretActionListeners.getMulticaster().afterAllCaretsAction();
  }

  @Override
  public void addCaretActionListener(@Nonnull CaretActionListener listener, @Nonnull Disposable disposable) {
    myCaretActionListeners.addListener(listener, disposable);
  }

  @Override
  public void runBatchCaretOperation(@Nonnull Runnable runnable) {
    CodeEditorBase.assertIsDispatchThread();
    doWithCaretMerging(runnable);
  }

  private void mergeOverlappingCaretsAndSelections() {
    CodeEditorBase.assertIsDispatchThread();
    if (myCarets.size() > 1) {
      LinkedList<CARET> carets = new LinkedList<>(myCarets);
      Collections.sort(carets, CARET_POSITION_COMPARATOR);
      ListIterator<CARET> it = carets.listIterator();
      CARET keepPrimary = getPrimaryCaret();
      while (it.hasNext()) {
        CARET prevCaret = null;
        if (it.hasPrevious()) {
          prevCaret = it.previous();
          it.next();
        }
        CARET currCaret = it.next();
        if (prevCaret != null && caretsOverlap(currCaret, prevCaret)) {
          int newSelectionStart = Math.min(currCaret.getSelectionStart(), prevCaret.getSelectionStart());
          int newSelectionEnd = Math.max(currCaret.getSelectionEnd(), prevCaret.getSelectionEnd());
          CARET toRetain;
          CARET toRemove;
          if (currCaret.getOffset() >= prevCaret.getSelectionStart() && currCaret.getOffset() <= prevCaret.getSelectionEnd()) {
            toRetain = prevCaret;
            toRemove = currCaret;
            it.remove();
            it.previous();
          }
          else {
            toRetain = currCaret;
            toRemove = prevCaret;
            it.previous();
            it.previous();
            it.remove();
          }
          if (toRemove == keepPrimary) {
            keepPrimary = toRetain;
          }
          removeCaret(toRemove);
          if (newSelectionStart < newSelectionEnd) {
            toRetain.setSelection(newSelectionStart, newSelectionEnd);
          }
        }
      }
      if (keepPrimary != getPrimaryCaret()) {
        synchronized (myCarets) {
          myCarets.remove(keepPrimary);
          myCarets.add(keepPrimary);
          myPrimaryCaret = keepPrimary;
        }
      }
    }
    if (myEditorSizeValidationScheduled) {
      myEditorSizeValidationScheduled = false;
      myEditor.validateSize();
    }
  }

  private boolean caretsOverlap(@Nonnull CARET firstCaret, @Nonnull CARET secondCaret) {
    if (firstCaret.getVisualPosition().equals(secondCaret.getVisualPosition())) {
      return true;
    }
    int firstStart = firstCaret.getSelectionStart();
    int secondStart = secondCaret.getSelectionStart();
    int firstEnd = firstCaret.getSelectionEnd();
    int secondEnd = secondCaret.getSelectionEnd();
    return firstStart < secondStart && firstEnd > secondStart ||
           firstStart > secondStart && firstStart < secondEnd ||
           firstStart == secondStart && secondEnd != secondStart && firstEnd > firstStart ||
           (hasPureVirtualSelection(firstCaret) || hasPureVirtualSelection(secondCaret)) && (firstStart == secondStart || firstEnd == secondEnd);
  }

  private boolean hasPureVirtualSelection(@Nonnull CARET firstCaret) {
    return firstCaret.getSelectionStart() == firstCaret.getSelectionEnd() && firstCaret.hasVirtualSelection();
  }

  public void doWithCaretMerging(@Nonnull Runnable runnable) {
    CodeEditorBase.assertIsDispatchThread();
    if (myPerformCaretMergingAfterCurrentOperation) {
      runnable.run();
    }
    else {
      myPerformCaretMergingAfterCurrentOperation = true;
      try {
        runnable.run();
        mergeOverlappingCaretsAndSelections();
      }
      finally {
        myPerformCaretMergingAfterCurrentOperation = false;
      }
    }
  }

  @Override
  public void setCaretsAndSelections(@Nonnull final List<? extends CaretState> caretStates) {
    setCaretsAndSelections(caretStates, true);
  }

  @Override
  public void setCaretsAndSelections(@Nonnull final List<? extends CaretState> caretStates, final boolean updateSystemSelection) {
    CodeEditorBase.assertIsDispatchThread();
    if (caretStates.isEmpty()) {
      throw new IllegalArgumentException("At least one caret should exist");
    }
    doWithCaretMerging(() -> {
      int index = 0;
      int oldCaretCount = myCarets.size();
      Iterator<CARET> caretIterator = myCarets.iterator();
      IntList selectionStartsBefore = null;
      IntList selectionStartsAfter = null;
      IntList selectionEndsBefore = null;
      IntList selectionEndsAfter = null;
      for (CaretState caretState : caretStates) {
        CARET caret;
        if (index++ < oldCaretCount) {
          caret = caretIterator.next();
          if (caretState != null && caretState.getCaretPosition() != null) {
            caret.moveToLogicalPosition(caretState.getCaretPosition());
          }
        }
        else {
          caret = createCaret(myEditor, this);
          if (caretState != null && caretState.getCaretPosition() != null) {
            caret.moveToLogicalPosition(caretState.getCaretPosition(), false, null, false, false);
          }
          synchronized (myCarets) {
            myCarets.add(caret);
            myPrimaryCaret = caret;
          }
          fireCaretAdded(caret);
        }
        if (caretState != null && caretState.getCaretPosition() != null && caretState.getVisualColumnAdjustment() != 0) {
          caret.myVisualColumnAdjustment = caretState.getVisualColumnAdjustment();
          caret.updateVisualPosition();
        }
        if (caretState != null && caretState.getSelectionStart() != null && caretState.getSelectionEnd() != null) {
          if (selectionStartsBefore == null) {
            int capacity = caretStates.size();
            selectionStartsBefore = IntLists.newArrayList(capacity);
            selectionStartsAfter = IntLists.newArrayList(capacity);
            selectionEndsBefore = IntLists.newArrayList(capacity);
            selectionEndsAfter = IntLists.newArrayList(capacity);
          }
          selectionStartsBefore.add(caret.getSelectionStart());
          selectionEndsBefore.add(caret.getSelectionEnd());
          caret.doSetSelection(myEditor.logicalToVisualPosition(caretState.getSelectionStart()), myEditor.logicalPositionToOffset(caretState.getSelectionStart()),
                               myEditor.logicalToVisualPosition(caretState.getSelectionEnd()), myEditor.logicalPositionToOffset(caretState.getSelectionEnd()), true, false, false);
          selectionStartsAfter.add(caret.getSelectionStart());
          selectionEndsAfter.add(caret.getSelectionEnd());
        }
      }
      int caretsToRemove = myCarets.size() - caretStates.size();
      for (int i = 0; i < caretsToRemove; i++) {
        CARET caret;
        synchronized (myCarets) {
          caret = myCarets.removeLast();
          myPrimaryCaret = myCarets.getLast();
        }
        fireCaretRemoved(caret);
        Disposer.dispose(caret);
      }
      if (updateSystemSelection) {
        updateSystemSelection();
      }
      if (selectionStartsBefore != null) {
        SelectionEvent event = new SelectionEvent(myEditor, selectionStartsBefore.toArray(), selectionEndsBefore.toArray(), selectionStartsAfter.toArray(), selectionEndsAfter.toArray());
        myEditor.getSelectionModel().fireSelectionChanged(event);
      }
    });
  }

  @Nonnull
  @Override
  public List<CaretState> getCaretsAndSelections() {
    synchronized (myCarets) {
      List<CaretState> states = new ArrayList<>(myCarets.size());
      for (CARET caret : myCarets) {
        states.add(new CaretState(caret.getLogicalPosition(), caret.myVisualColumnAdjustment, caret.getSelectionStartLogicalPosition(), caret.getSelectionEndLogicalPosition()));
      }
      return states;
    }
  }

  void updateSystemSelection() {
    if (GraphicsEnvironment.isHeadless()) return;

    final Clipboard clip = myEditor.getComponent().getToolkit().getSystemSelection();
    if (clip != null) {
      clip.setContents(new StringSelection(myEditor.getSelectionModel().getSelectedText(true)), EmptyClipboardOwner.INSTANCE);
    }
  }

  void fireCaretPositionChanged(@Nonnull CaretEvent caretEvent) {
    myCaretListeners.getMulticaster().caretPositionChanged(caretEvent);
  }

  void validateEditorSize() {
    if (myEditor.getSettings().isVirtualSpace()) {
      if (myPerformCaretMergingAfterCurrentOperation) {
        myEditorSizeValidationScheduled = true;
      }
      else {
        myEditor.validateSize();
      }
    }
  }

  private void fireCaretAdded(@Nonnull Caret caret) {
    myCaretListeners.getMulticaster().caretAdded(new CaretEvent(caret, caret.getLogicalPosition(), caret.getLogicalPosition()));
  }

  private void fireCaretRemoved(@Nonnull Caret caret) {
    myCaretListeners.getMulticaster().caretRemoved(new CaretEvent(caret, caret.getLogicalPosition(), caret.getLogicalPosition()));
  }

  public boolean isIteratingOverCarets() {
    return myCurrentCaret.get() != null;
  }

  @Nonnull
  @Override
  public String dumpState() {
    return "[in update: " +
           myIsInUpdate +
           ", update counter: " +
           myDocumentUpdateCounter +
           ", perform caret merging: " +
           myPerformCaretMergingAfterCurrentOperation +
           ", current caret: " +
           myCurrentCaret.get() +
           ", all carets: " +
           ContainerUtil.map(myCarets, CARET::dumpState) +
           "]";
  }

  @Override
  public void onAdded(@Nonnull Inlay inlay) {
    if (myEditor.getDocument().isInBulkUpdate()) return;
    Inlay.Placement placement = inlay.getPlacement();
    if (placement == Inlay.Placement.INLINE) {
      int offset = inlay.getOffset();
      for (CARET caret : myCarets) {
        caret.onInlayAdded(offset);
      }
    }
    else if (placement != Inlay.Placement.AFTER_LINE_END || hasCaretInVirtualSpace()) {
      updateVisualPosition();
    }
  }

  @Override
  public void onRemoved(@Nonnull Inlay inlay) {
    if (myEditor.getDocument().isInBulkUpdate()) return;
    Inlay.Placement placement = inlay.getPlacement();
    if (myEditor.getDocument().isInEventsHandling()) {
      if (placement == Inlay.Placement.AFTER_LINE_END) myVisualPositionUpdateScheduled = true;
      return;
    }
    if (placement == Inlay.Placement.INLINE) {
      doWithCaretMerging(() -> {
        for (CARET caret : myCarets) {
          caret.onInlayRemoved(inlay.getOffset(), ((InlineInlayImpl)inlay).getOrder());
        }
      });
    }
    else if (placement != Inlay.Placement.AFTER_LINE_END || hasCaretInVirtualSpace()) {
      updateVisualPosition();
    }
  }

  @Override
  public void onUpdated(@Nonnull Inlay inlay) {
    if (myEditor.getDocument().isInBulkUpdate()) return;
    if (inlay.getPlacement() != Inlay.Placement.AFTER_LINE_END || hasCaretInVirtualSpace()) {
      updateVisualPosition();
    }
  }

  private boolean hasCaretInVirtualSpace() {
    return myEditor.getSettings().isVirtualSpace() && ContainerUtil.exists(myCarets, CARET::isInVirtualSpace);
  }

  @TestOnly
  public void validateState() {
    for (CARET caret : myCarets) {
      caret.validateState();
    }
  }

  private static final Comparator<VisualPosition> VISUAL_POSITION_COMPARATOR = (o1, o2) -> {
    if (o1.line != o2.line) {
      return o1.line - o2.line;
    }
    return o1.column - o2.column;
  };

  private static final Comparator<Caret> CARET_POSITION_COMPARATOR = (o1, o2) -> VISUAL_POSITION_COMPARATOR.compare(o1.getVisualPosition(), o2.getVisualPosition());

}
