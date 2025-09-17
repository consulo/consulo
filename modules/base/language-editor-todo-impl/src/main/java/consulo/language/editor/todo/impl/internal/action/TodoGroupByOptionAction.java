/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.todo.impl.internal.action;

import consulo.language.editor.todo.impl.internal.TodoPanel;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static consulo.language.editor.todo.impl.internal.TodoPanel.TODO_PANEL_DATA_KEY;

/**
 * @author UNV
 * @since 2025-09-17
 */
public abstract class TodoGroupByOptionAction extends ToggleAction {
    private final Function<TodoPanel, Boolean> myGetter;
    @RequiredUIAccess
    private final BiConsumer<TodoPanel, Boolean> mySetter;

    protected TodoGroupByOptionAction(
        LocalizeValue text,
        Image icon,
        Function<TodoPanel, Boolean> getter,
        @RequiredUIAccess BiConsumer<TodoPanel, Boolean> setter
    ) {
        super(text, LocalizeValue.empty(), icon);
        myGetter = getter;
        mySetter = setter;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        if (!e.hasData(TODO_PANEL_DATA_KEY)) {
            e.getPresentation().setEnabled(false);
        }
        super.update(e);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
        return todoPanel != null && myGetter.apply(todoPanel);
    }

    @Override
    @RequiredUIAccess
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
        if (todoPanel != null) {
            mySetter.accept(todoPanel, state);
        }
    }
}
