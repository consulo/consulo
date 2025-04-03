/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ide.impl.idea.openapi.keymap.ex.KeymapManagerEx;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class QuickChangeKeymapAction extends QuickSwitchSchemeAction {
    @Override
    protected void fillActions(Project project, @Nonnull DefaultActionGroup group, @Nonnull DataContext dataContext) {
        KeymapManagerEx manager = (KeymapManagerEx)KeymapManager.getInstance();
        Keymap current = manager.getActiveKeymap();
        for (Keymap keymap : manager.getAllKeymaps()) {
            addKeymapAction(group, manager, current, keymap, false);
        }
    }

    private static void addKeymapAction(
        final DefaultActionGroup group,
        final KeymapManagerEx manager,
        final Keymap current,
        final Keymap keymap,
        final boolean addScheme
    ) {
        group.add(new AnAction(keymap.getPresentableName(), "", keymap == current ? ourCurrentAction : ourNotCurrentAction) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                if (addScheme) {
                    manager.getSchemeManager().addNewScheme(keymap, false);
                }
                manager.setActiveKeymap(keymap);
            }
        });
    }

    @Override
    protected boolean isEnabled() {
        return ((KeymapManagerEx)KeymapManager.getInstance()).getAllKeymaps().length > 1;
    }
}
