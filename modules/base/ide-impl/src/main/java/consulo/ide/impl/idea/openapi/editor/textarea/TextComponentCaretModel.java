// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.editor.textarea;

import consulo.codeEditor.*;
import consulo.codeEditor.event.CaretActionListener;
import consulo.codeEditor.event.CaretListener;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.util.EventDispatcher;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class TextComponentCaretModel implements CaretModel {
    private final JTextComponent myTextComponent;
    private final TextComponentEditor myEditor;
    private final Caret myCaret;
    private final EventDispatcher<CaretActionListener> myCaretActionListeners = EventDispatcher.create(CaretActionListener.class);

    public TextComponentCaretModel(@Nonnull JTextComponent textComponent, @Nonnull TextComponentEditor editor) {
        myTextComponent = textComponent;
        myEditor = editor;
        myCaret = new TextComponentCaret(editor);
    }

    @Override
    public void moveCaretRelatively(final int columnShift, final int lineShift, final boolean withSelection, final boolean blockSelection, final boolean scrollToCaret) {
        if (lineShift == 0 && !withSelection && !blockSelection && !scrollToCaret) {
            moveToOffset(getOffset() + columnShift);
            return;
        }
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void moveToLogicalPosition(@Nonnull final LogicalPosition pos) {
        moveToOffset(myEditor.logicalPositionToOffset(pos), false);
    }

    @Override
    public void moveToVisualPosition(@Nonnull final VisualPosition pos) {
        moveToLogicalPosition(myEditor.visualToLogicalPosition(pos));
    }

    @Override
    public void moveToOffset(int offset) {
        moveToOffset(offset, false);
    }

    @Override
    public void moveToOffset(final int offset, boolean locateBeforeSoftWrap) {
        int targetOffset = Math.min(offset, myTextComponent.getText().length());
        int currentPosition = myTextComponent.getCaretPosition();
        // We try to preserve selection, to match EditorImpl behaviour.
        // It's only possible though, if target offset is located at either end of existing selection.
        if (targetOffset != currentPosition) {
            if (targetOffset == myTextComponent.getCaret().getMark()) {
                myTextComponent.setCaretPosition(currentPosition);
                myTextComponent.moveCaretPosition(targetOffset);
            }
            else {
                myTextComponent.setCaretPosition(targetOffset);
            }
        }
    }

    @Override
    public boolean isUpToDate() {
        return true;
    }

    @Override
    @Nonnull
    public LogicalPosition getLogicalPosition() {
        int caretPos = myTextComponent.getCaretPosition();
        int line;
        int lineStart;
        if (myTextComponent instanceof JTextArea) {
            final JTextArea textArea = (JTextArea) myTextComponent;
            try {
                line = textArea.getLineOfOffset(caretPos);
                lineStart = textArea.getLineStartOffset(line);
            }
            catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            line = 0;
            lineStart = 0;
        }
        return new LogicalPosition(line, caretPos - lineStart);
    }

    @Override
    @Nonnull
    public VisualPosition getVisualPosition() {
        LogicalPosition pos = getLogicalPosition();
        return new VisualPosition(pos.line, pos.column);
    }

    @Override
    public int getOffset() {
        return myTextComponent.getCaretPosition();
    }

    @Override
    public void addCaretListener(@Nonnull final CaretListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void removeCaretListener(@Nonnull final CaretListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getVisualLineStart() {
        return 0;
    }

    @Override
    public int getVisualLineEnd() {
        return 0;
    }

    @Override
    public TextAttributes getTextAttributes() {
        return null;
    }

    @Override
    public boolean supportsMultipleCarets() {
        return false;
    }

    @Override
    public int getMaxCaretCount() {
        return 1;
    }

    @Nonnull
    @Override
    public Caret getCurrentCaret() {
        return myCaret;
    }

    @Nonnull
    @Override
    public Caret getPrimaryCaret() {
        return myCaret;
    }

    @Override
    public int getCaretCount() {
        return 1;
    }

    @Nonnull
    @Override
    public List<Caret> getAllCarets() {
        return Collections.singletonList(myCaret);
    }

    @Nullable
    @Override
    public Caret getCaretAt(@Nonnull VisualPosition pos) {
        return myCaret.getVisualPosition().equals(pos) ? myCaret : null;
    }

    @Nullable
    @Override
    public Caret addCaret(@Nonnull VisualPosition pos) {
        return null;
    }

    @Nullable
    @Override
    public Caret addCaret(@Nonnull VisualPosition pos, boolean makePrimary) {
        return null;
    }

    @Override
    public boolean removeCaret(@Nonnull Caret caret) {
        return false;
    }

    @Override
    public void removeSecondaryCarets() {
    }

    @Override
    public void setCaretsAndSelections(@Nonnull List<? extends CaretState> caretStates) {
        if (caretStates.size() != 1) {
            throw new IllegalArgumentException("Exactly one CaretState object must be passed");
        }
        CaretState state = caretStates.get(0);
        if (state != null) {
            if (state.getCaretPosition() != null) {
                moveToLogicalPosition(state.getCaretPosition());
            }
            if (state.getSelectionStart() != null && state.getSelectionEnd() != null) {
                myEditor.getSelectionModel().setSelection(myEditor.logicalPositionToOffset(state.getSelectionStart()), myEditor.logicalPositionToOffset(state.getSelectionEnd()));
            }
        }
    }

    @Override
    public void setCaretsAndSelections(@Nonnull List<? extends CaretState> caretStates, boolean updateSystemSelection) {
        setCaretsAndSelections(caretStates);
    }

    @Nonnull
    @Override
    public List<CaretState> getCaretsAndSelections() {
        return Collections.singletonList(new CaretState(getLogicalPosition(), myEditor.offsetToLogicalPosition(myEditor.getSelectionModel().getSelectionStart()),
            myEditor.offsetToLogicalPosition(myEditor.getSelectionModel().getSelectionEnd())));
    }

    @Override
    public void runForEachCaret(@Nonnull CaretAction action) {
        myCaretActionListeners.getMulticaster().beforeAllCaretsAction();
        action.perform(myCaret);
        myCaretActionListeners.getMulticaster().afterAllCaretsAction();
    }

    @Override
    public void runForEachCaret(@Nonnull CaretAction action, boolean reverseOrder) {
        runForEachCaret(action);
    }

    @Override
    public void addCaretActionListener(@Nonnull CaretActionListener listener, @Nonnull Disposable disposable) {
        myCaretActionListeners.addListener(listener, disposable);
    }

    @Override
    public void runBatchCaretOperation(@Nonnull Runnable runnable) {
        runnable.run();
    }
}
