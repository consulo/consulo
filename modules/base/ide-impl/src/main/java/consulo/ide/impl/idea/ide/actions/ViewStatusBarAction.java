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

import consulo.application.ui.UISettings;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.application.dumb.DumbAware;
import jakarta.annotation.Nonnull;

/**
 * @author Vladimir Kondratyev
 */
public class ViewStatusBarAction extends ToggleAction implements DumbAware {
    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return UISettings.getInstance().SHOW_STATUS_BAR;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        UISettings uiSettings = UISettings.getInstance();
        uiSettings.SHOW_STATUS_BAR = state;
        uiSettings.fireUISettingsChanged();
    }
}
