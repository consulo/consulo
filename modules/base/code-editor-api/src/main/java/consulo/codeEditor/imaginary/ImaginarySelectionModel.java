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

import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.event.SelectionListener;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2025-03-07
 */
public class ImaginarySelectionModel implements SelectionModel {
    private static final Logger LOG = Logger.getInstance(ImaginarySelectionModel.class);

    private final ImaginaryEditor myEditor;

    public ImaginarySelectionModel(ImaginaryEditor editor) {
        myEditor = editor;
    }

    @Override
    public int getSelectionStart() {
        return myEditor.getCaretModel().getCurrentCaret().getSelectionStart();
    }

    @Nullable
    @Override
    public VisualPosition getSelectionStartPosition() {
        return myEditor.getCaretModel().getCurrentCaret().getSelectionStartPosition();
    }

    @Override
    public int getSelectionEnd() {
        return myEditor.getCaretModel().getCurrentCaret().getSelectionEnd();
    }

    @Nullable
    @Override
    public VisualPosition getSelectionEndPosition() {
        return myEditor.getCaretModel().getCurrentCaret().getSelectionEndPosition();
    }

    @Nullable
    @Override
    public String getSelectedText() {
        return myEditor.getCaretModel().getCurrentCaret().getSelectedText();
    }

    @Nullable
    @Override
    public String getSelectedText(boolean allCarets) {
        return myEditor.getDocument().getText(TextRange.create(getSelectionStart(), getSelectionEnd()));
    }

    @Override
    public int getLeadSelectionOffset() {
        return myEditor.getCaretModel().getCurrentCaret().getLeadSelectionOffset();
    }

    @Nullable
    @Override
    public VisualPosition getLeadSelectionPosition() {
        return myEditor.getCaretModel().getCurrentCaret().getLeadSelectionPosition();
    }

    @Override
    public boolean hasSelection() {
        return myEditor.getCaretModel().getCurrentCaret().hasSelection();
    }

    @Override
    public boolean hasSelection(boolean anyCaret) {
        return hasSelection();
    }

    @Override
    public void setSelection(int startOffset, int endOffset) {
        myEditor.getCaretModel().getCurrentCaret().setSelection(startOffset, endOffset);
    }

    @Override
    public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
        throw myEditor.notImplemented();
    }

    @Override
    public void setSelection(@Nullable VisualPosition startPosition, int startOffset,
                             @Nullable VisualPosition endPosition, int endOffset) {
        throw myEditor.notImplemented();
    }

    @Override
    public void removeSelection() {
        myEditor.getCaretModel().getCurrentCaret().removeSelection();
    }

    @Override
    public void removeSelection(boolean allCarets) {
        removeSelection();
    }

    @Override
    public void selectLineAtCaret() {
        throw myEditor.notImplemented();
    }

    @Override
    public void selectWordAtCaret(boolean honorCamelWordsSettings) {
        throw myEditor.notImplemented();
    }

    @Override
    public void copySelectionToClipboard() {
        throw myEditor.notImplemented();
    }

    @Override
    public void setBlockSelection(LogicalPosition blockStart, LogicalPosition blockEnd) {
        throw myEditor.notImplemented();
    }

    @Override
    public int[] getBlockSelectionStarts() {
        return new int[]{getSelectionStart()};
    }

    @Override
    public int[] getBlockSelectionEnds() {
        return new int[]{getSelectionEnd()};
    }

    @Override
    public TextAttributes getTextAttributes() {
        throw myEditor.notImplemented();
    }

    @Override
    public void addSelectionListener(SelectionListener listener) {
        LOG.info("Called ImaginarySelectionModel#addSelectionListener which is stubbed and has no implementation");
    }

    @Override
    public void removeSelectionListener(SelectionListener listener) {
        LOG.info("Called ImaginarySelectionModel#removeSelectionListener which is stubbed and has no implementation");
    }
}
