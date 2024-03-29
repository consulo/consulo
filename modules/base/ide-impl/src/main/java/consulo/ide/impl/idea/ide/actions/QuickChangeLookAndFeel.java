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

import consulo.ide.impl.idea.ide.ui.LafManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author max
 */
public class QuickChangeLookAndFeel extends QuickSwitchSchemeAction {
  @Override
  protected void fillActions(Project project, @Nonnull DefaultActionGroup group, @Nonnull DataContext dataContext) {
    final LafManager manager = LafManager.getInstance();
    final UIManager.LookAndFeelInfo[] lfs = manager.getInstalledLookAndFeels();
    final UIManager.LookAndFeelInfo current = manager.getCurrentLookAndFeel();
    for (final UIManager.LookAndFeelInfo lf : lfs) {
      group.add(new DumbAwareAction(lf.getName(), "", lf == current ? ourCurrentAction : ourNotCurrentAction) {
        @Override
        public void actionPerformed(AnActionEvent e) {
          final UIManager.LookAndFeelInfo cur = manager.getCurrentLookAndFeel();
          if (cur == lf) return;
          manager.setCurrentLookAndFeel(lf);
          manager.updateUI();
        }
      });
    }
  }

  @Override
  protected boolean isEnabled() {
    return LafManager.getInstance().getInstalledLookAndFeels().length > 1;
  }
}
