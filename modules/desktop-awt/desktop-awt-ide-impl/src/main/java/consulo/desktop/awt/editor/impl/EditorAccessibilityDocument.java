/*
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
package consulo.desktop.awt.editor.impl;

import consulo.application.Application;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@linkplain javax.swing.text.PlainDocument} does a lot of work we don't need.
 * This exists simply to be able to send editing events to the screen reader.
 */
@SuppressWarnings("UnnecessaryFullyQualifiedName")
final class EditorAccessibilityDocument implements javax.swing.text.Document, javax.swing.text.Element {
    private final DesktopEditorContentUIComponent myComponent;
    private final DesktopEditorImpl myEditor;

    EditorAccessibilityDocument(DesktopEditorContentUIComponent uiComponent) {
        myComponent = uiComponent;
        myEditor = uiComponent.getEditor();
    }

    private List<javax.swing.event.DocumentListener> myListeners;

    public @Nullable List<javax.swing.event.DocumentListener> getListeners() {
        return myListeners;
    }

    @Override
    public int getLength() {
        return myEditor.getDocument().getTextLength();
    }

    @Override
    public void addDocumentListener(javax.swing.event.DocumentListener documentListener) {
        if (myListeners == null) {
            myListeners = new ArrayList<>(2);
        }
        myListeners.add(documentListener);
    }

    @Override
    public void removeDocumentListener(javax.swing.event.DocumentListener documentListener) {
        if (myListeners != null) {
            myListeners.remove(documentListener);
        }
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener undoableEditListener) {
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener undoableEditListener) {
    }

    @Override
    public @Nullable Object getProperty(Object o) {
        return null;
    }

    @Override
    public void putProperty(Object o, Object o1) {
    }

    @Override
    @RequiredUIAccess
    public void remove(int offset, int length) throws BadLocationException {
        myComponent.editDocumentSafely(offset, length, null);
    }

    @Override
    @RequiredUIAccess
    public void insertString(int offset, String text, AttributeSet attributeSet) throws BadLocationException {
        myComponent.editDocumentSafely(offset, 0, text);
    }

    @Override
    public String getText(int offset, int length) throws BadLocationException {
        return Application.get().runReadAction(
            (Supplier<String>)() -> myEditor.getDocument().getText(new TextRange(offset, offset + length))
        );
    }

    @Override
    public void getText(int offset, int length, Segment segment) throws BadLocationException {
        char[] s = getText(offset, length).toCharArray();
        segment.array = s;
        segment.offset = 0;
        segment.count = s.length;
    }

    @Override
    public @Nullable Position getStartPosition() {
        DesktopEditorContentUIComponent.notSupported();
        return null;
    }

    @Override
    public @Nullable Position getEndPosition() {
        DesktopEditorContentUIComponent.notSupported();
        return null;
    }

    @Override
    public @Nullable Position createPosition(int i) throws BadLocationException {
        DesktopEditorContentUIComponent.notSupported();
        return null;
    }

    @Override
    public Element[] getRootElements() {
        return new Element[]{this};
    }

    @Override
    public Element getDefaultRootElement() {
        return this;
    }

    @Override
    public void render(Runnable runnable) {
        Application.get().runReadAction(runnable);
    }

    // ---- Implements Element for the root element ----
    //
    // This is here because the accessibility code ends up calling some JTextComponent
    // methods; in particular, CAccessibleText calls root.getElementIndex(index)
    // to map an offset to a line number, and getRangeForLine calls root.getElement(lineIndex)
    // to get a range object for a given line, and then getStartOffset() and getEndOffset()
    // on the result.

    @Override
    public javax.swing.text.Document getDocument() {
        return this;
    }

    @Override
    public @Nullable Element getParentElement() {
        return null;
    }

    @Override
    public @Nullable String getName() {
        return null;
    }

    @Override
    public @Nullable AttributeSet getAttributes() {
        return null;
    }

    @Override
    public int getStartOffset() {
        return 0;
    }

    @Override
    public int getEndOffset() {
        return getLength();
    }

    @Override
    public int getElementIndex(int i) {
        // For the root element this asks for the index of the offset, which
        // means the line number
        Document document = myEditor.getDocument();
        return document.getLineNumber(i);
    }

    @Override
    public int getElementCount() {
        Document document = myEditor.getDocument();
        return document.getLineCount();
    }

    @Override
    public Element getElement(final int i) {
        return new Element() {
            @Override
            public javax.swing.text.Document getDocument() {
                return EditorAccessibilityDocument.this;
            }

            @Override
            public Element getParentElement() {
                return EditorAccessibilityDocument.this;
            }

            @Override
            public @Nullable String getName() {
                return null;
            }

            @Override
            public @Nullable AttributeSet getAttributes() {
                return null;
            }

            @Override
            public int getStartOffset() {
                Document document = myEditor.getDocument();
                return document.getLineStartOffset(i);
            }

            @Override
            public int getEndOffset() {
                Document document = myEditor.getDocument();
                return document.getLineEndOffset(i);
            }

            @Override
            public int getElementIndex(int i) {
                return 0;
            }

            @Override
            public int getElementCount() {
                return 0;
            }

            @Override
            public @Nullable Element getElement(int i) {
                return null;
            }

            @Override
            public boolean isLeaf() {
                return true;
            }
        };
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
