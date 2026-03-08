/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 * Copyright 2013-2026 consulo.io
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
package consulo.codeEditor.imaginary;

import consulo.codeEditor.*;
import consulo.codeEditor.event.CaretActionListener;
import consulo.codeEditor.event.CaretListener;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2025-03-07
 */
public class ImaginaryCaretModel implements CaretModel {
    private static final Logger LOG = Logger.getInstance(ImaginaryCaretModel.class);

    private final ImaginaryEditor myEditor;
    private final ImaginaryCaret myCaret;

    public ImaginaryCaretModel(ImaginaryEditor editor) {
        myEditor = editor;
        myCaret = new ImaginaryCaret(this);
    }

    protected ImaginaryEditor getEditor() {
        return myEditor;
    }

    protected RuntimeException notImplemented() {
        return myEditor.notImplemented();
    }

    @Override
    public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean blockSelection, boolean scrollToCaret) {
        myCaret.moveCaretRelatively(columnShift, lineShift, withSelection, scrollToCaret);
    }

    @Override
    public void moveToLogicalPosition(@Nonnull LogicalPosition pos) {
        myCaret.moveToLogicalPosition(pos);
    }

    @Override
    public void moveToVisualPosition(@Nonnull VisualPosition pos) {
        myCaret.moveToVisualPosition(pos);
    }

    @Override
    public void moveToOffset(int offset) {
        myCaret.moveToOffset(offset);
    }

    @Override
    public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {
        myCaret.moveToOffset(offset, locateBeforeSoftWrap);
    }

    @Override
    public boolean isUpToDate() {
        return true;
    }

    @Nonnull
    @Override
    public LogicalPosition getLogicalPosition() {
        return myCaret.getLogicalPosition();
    }

    @Nonnull
    @Override
    public VisualPosition getVisualPosition() {
        return myCaret.getVisualPosition();
    }

    @Override
    public int getOffset() {
        return myCaret.getOffset();
    }

    @Override
    public int getVisualLineStart() {
        throw notImplemented();
    }

    @Override
    public int getVisualLineEnd() {
        throw notImplemented();
    }

    @Override
    public TextAttributes getTextAttributes() {
        throw notImplemented();
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
        return getPrimaryCaret();
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
        throw notImplemented();
    }

    @Nullable
    @Override
    public Caret addCaret(@Nonnull VisualPosition pos) {
        throw notImplemented();
    }

    @Nullable
    @Override
    public Caret addCaret(@Nonnull VisualPosition pos, boolean makePrimary) {
        throw notImplemented();
    }

    @Override
    public boolean removeCaret(@Nonnull Caret caret) {
        throw notImplemented();
    }

    @Override
    public void removeSecondaryCarets() {
    }

    @Override
    public void setCaretsAndSelections(@Nonnull List<? extends CaretState> caretStates) {
        setCaretsAndSelections(caretStates, true);
    }

    @Override
    public void setCaretsAndSelections(@Nonnull List<? extends CaretState> caretStates, boolean updateSystemSelection) {
        if (caretStates.size() != 1) {
            LOG.error("Imaginary caret does not support multicaret. caretStates=" + caretStates);
        }
        CaretState state = caretStates.get(0);
        if (state.getCaretPosition() != null) {
            getCurrentCaret().moveToOffset(myEditor.logicalPositionToOffset(state.getCaretPosition()));
        }
        if (state.getSelectionStart() != null && state.getSelectionEnd() != null
            && !state.getSelectionStart().equals(state.getSelectionEnd())) {
            getCurrentCaret().setSelection(
                myEditor.logicalPositionToOffset(state.getSelectionStart()),
                myEditor.logicalPositionToOffset(state.getSelectionEnd()));
        }
    }

    @Nonnull
    @Override
    public List<CaretState> getCaretsAndSelections() {
        return Collections.singletonList(
            new CaretState(
                getCurrentCaret().getLogicalPosition(),
                0,
                myEditor.offsetToLogicalPosition(getCurrentCaret().getSelectionStart()),
                myEditor.offsetToLogicalPosition(getCurrentCaret().getSelectionEnd())
            )
        );
    }

    @Override
    public void runForEachCaret(@Nonnull CaretAction action) {
        action.perform(getCurrentCaret());
    }

    @Override
    public void runForEachCaret(@Nonnull CaretAction action, boolean reverseOrder) {
        action.perform(getCurrentCaret());
    }

    @Override
    public void addCaretActionListener(@Nonnull CaretActionListener listener, @Nonnull Disposable disposable) {
        throw notImplemented();
    }

    @Override
    public void runBatchCaretOperation(@Nonnull Runnable runnable) {
        throw notImplemented();
    }

    @Override
    public void addCaretListener(@Nonnull CaretListener listener) {
        LOG.info("Called ImaginaryCaretModel#addCaretListener which is stubbed and has no implementation");
    }

    @Override
    public void removeCaretListener(@Nonnull CaretListener listener) {
        LOG.info("Called ImaginaryCaretModel#removeCaretListener which is stubbed and has no implementation");
    }
}
