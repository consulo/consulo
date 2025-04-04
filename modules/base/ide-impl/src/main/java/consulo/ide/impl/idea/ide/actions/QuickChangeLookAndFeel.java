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

import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author max
 */
public class QuickChangeLookAndFeel extends QuickSwitchSchemeAction {
    @Override
    protected void fillActions(Project project, @Nonnull DefaultActionGroup group, @Nonnull DataContext dataContext) {
        StyleManager styleManager = StyleManager.get();
        List<Style> styles = styleManager.getStyles();
        Style currentStyle = styleManager.getCurrentStyle();
        for (Style newStyle : styles) {
            group.add(new DumbAwareAction(
                LocalizeValue.of(newStyle.getName()),
                LocalizeValue.of(),
                newStyle == currentStyle ? ourCurrentAction : ourNotCurrentAction
            ) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    StyleManager.get().setCurrentStyle(newStyle);
                }
            });
        }
    }

    @Override
    protected boolean isEnabled() {
        return StyleManager.get().getStyles().size() > 1;
    }
}
