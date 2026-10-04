// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.ui.grid.editor;

import consulo.disposer.Disposable;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The editor of a cell has no widget of its own. It describes the widget the frontend shows with {@link #getPresentation()}, and
 * the frontend reports what the user does in it: {@link #setText} on every change of the text, and {@link #selectOption} for a
 * choice of a {@link GridCellEditorPresentation.Kind#LIST}.
 */
public interface GridCellEditor extends Disposable {
    /**
     * The widget the frontend shows for the editor.
     */
    GridCellEditorPresentation getPresentation();

    default boolean shouldMoveFocus() {
        return true;
    }

    @Nullable
    Object getValue();

    String getText();

    /**
     * The text of the hosted field changed. A text editor clears its
     * {@link #getError() error} and fires the new value to the {@link #setEditingListener editing listener}.
     */
    void setText(String text);

    /**
     * Applies the option with this index of {@link GridCellEditorPresentation#options()}: the value object of the option, not its
     * text. The grid commits the editor afterwards.
     */
    default void selectOption(int index) {
        throw new UnsupportedOperationException(getClass().getName() + " has no options");
    }

    /**
     * This method is called by a grid which uses this editor, when editing stops. <br/>
     * If you want to stop editing, call {@link consulo.ui.grid.CoreGrid#stopEditing()}.
     *
     * @return true, if editing can be stopped, false otherwise.
     */
    boolean stop();

    /**
     * This method is called by a grid which uses this editor, when editing cancels. <br/>
     * If you want to cancel editing, call {@link consulo.ui.grid.CoreGrid#cancelEditing()}
     */
    void cancel();

    /**
     * Why the last {@link #stop()} refused, or {@code null}. The frontend shows it: a red outline, an error highlight from the offset
     * to the end of the text, and a tooltip with the message. It is cleared by {@link #setText}.
     */
    default UnparsedValue.@Nullable ParsingError getError() {
        return null;
    }

    boolean isColumnSpanAllowed();

    void setEditingListener(Consumer<@Nullable Object> listener);

    abstract class Adapter implements GridCellEditor {
        private @Nullable Consumer<@Nullable Object> myEditingListener;

        @Override
        public boolean stop() {
            return true;
        }

        @Override
        public void cancel() {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isColumnSpanAllowed() {
            return true;
        }

        @Override
        public void setEditingListener(Consumer<@Nullable Object> listener) {
            myEditingListener = listener;
        }

        protected final void fireEditing(@Nullable Object object) {
            if (myEditingListener != null) {
                myEditingListener.accept(object);
            }
        }
    }
}
