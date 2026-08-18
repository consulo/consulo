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
package consulo.desktop.awt.editor.impl.internal;

import org.jspecify.annotations.Nullable;

import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.Graphics;
import java.awt.Point;

/**
 * {@linkplain DefaultCaret} does a lot of work we don't want (listening
 * for focus events etc). This exists simply to be able to send caret events to the screen reader.
 */
final class EditorAccessibilityCaret implements javax.swing.text.Caret {
    private final DesktopEditorImpl myEditor;

    EditorAccessibilityCaret(DesktopEditorImpl editor) {
        myEditor = editor;
    }

    @Override
    public void install(JTextComponent jTextComponent) {
    }

    @Override
    public void deinstall(JTextComponent jTextComponent) {
    }

    @Override
    public void paint(Graphics graphics) {
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void setVisible(boolean visible) {
    }

    @Override
    public boolean isSelectionVisible() {
        return true;
    }

    @Override
    public void setSelectionVisible(boolean visible) {
    }

    @Override
    public void setMagicCaretPosition(Point point) {
    }

    @Override
    public @Nullable Point getMagicCaretPosition() {
        return null;
    }

    @Override
    public void setBlinkRate(int rate) {
    }

    @Override
    public int getBlinkRate() {
        return 250;
    }

    @Override
    public int getDot() {
        return myEditor.getCaretModel().getOffset();
    }

    @Override
    public int getMark() {
        return myEditor.getSelectionModel().getSelectionStart();
    }

    @Override
    public void setDot(int offset) {
        if (!myEditor.isDisposed()) {
            myEditor.getCaretModel().moveToOffset(offset);
        }
    }

    @Override
    public void moveDot(int offset) {
        if (!myEditor.isDisposed()) {
            myEditor.getCaretModel().moveToOffset(offset);
        }
    }
}
