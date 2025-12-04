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

import consulo.annotation.component.ActionImpl;
import consulo.language.editor.todo.impl.internal.TodoPanel;
import consulo.language.editor.todo.impl.internal.localize.LanguageTodoLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

import static consulo.language.editor.todo.impl.internal.TodoPanel.TODO_PANEL_DATA_KEY;

/**
 * @author UNV
 * @since 2025-09-17
 */
@ActionImpl(id = "TodoViewGroupByFlattenPackage")
public class TodoGroupByFlattenPackages extends TodoGroupByOptionAction {
    public TodoGroupByFlattenPackages() {
        super(
            LanguageTodoLocalize.actionFlattenView().map(text -> "   " + text),
            PlatformIconGroup.objectbrowserFlattenpackages(),
            TodoPanel::isFlattenPackages,
            TodoPanel::setFlattenPackages
        );
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        TodoPanel todoPanel = e.getData(TODO_PANEL_DATA_KEY);
        e.getPresentation().setEnabled(todoPanel != null && todoPanel.isPackagesShown());
    }
}
