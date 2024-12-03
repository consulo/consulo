/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.find.SearchSession;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-11-21
 */
public abstract class EditorSearchToggleAction extends ToggleAction implements Embeddable, DumbAware {
    protected EditorSearchToggleAction(
        @Nonnull LocalizeValue text,
        @Nullable Image icon,
        @Nullable Image hoveredIcon,
        @Nullable Image selectedIcon
    ) {
        super(text);
        getTemplatePresentation().setIcon(icon);
        getTemplatePresentation().setHoveredIcon(hoveredIcon);
        getTemplatePresentation().setSelectedIcon(selectedIcon);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        SearchSession search = e.getData(SearchSession.KEY);
        return search != null && isSelected(search);
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean selected) {
        SearchSession search = e.getData(SearchSession.KEY);
        if (search != null) {
            setSelected(search, selected);
        }
    }

    protected abstract boolean isSelected(@Nonnull SearchSession session);

    protected abstract void setSelected(@Nonnull SearchSession session, boolean selected);
}
