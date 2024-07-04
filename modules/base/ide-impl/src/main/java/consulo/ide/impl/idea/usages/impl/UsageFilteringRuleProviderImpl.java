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
package consulo.ide.impl.idea.usages.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.usages.impl.rules.ReadAccessFilteringRule;
import consulo.ide.impl.idea.usages.impl.rules.WriteAccessFilteringRule;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.ToggleAction;
import consulo.usage.UsageView;
import consulo.usage.UsageViewBundle;
import consulo.usage.rule.UsageFilteringRule;
import consulo.usage.rule.UsageFilteringRuleListener;
import consulo.usage.rule.UsageFilteringRuleProvider;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
@ExtensionImpl
public class UsageFilteringRuleProviderImpl implements UsageFilteringRuleProvider {
  private final ReadWriteState myReadWriteState = new ReadWriteState();

  @Override
  @Nonnull
  public UsageFilteringRule[] getActiveRules(@Nonnull Project project) {
    final List<UsageFilteringRule> rules = new ArrayList<>();

    if (!myReadWriteState.isShowReadAccess()) {
      rules.add(new ReadAccessFilteringRule());
    }
    if (!myReadWriteState.isShowWriteAccess()) {
      rules.add(new WriteAccessFilteringRule());
    }
    return rules.toArray(new UsageFilteringRule[rules.size()]);
  }

  @Override
  @Nonnull
  public AnAction[] createFilteringActions(@Nonnull UsageView view) {
    if (!view.getPresentation().isCodeUsages()) {
      return AnAction.EMPTY_ARRAY;
    }
    final JComponent component = view.getComponent();

    final ShowReadAccessUsagesAction read = new ShowReadAccessUsagesAction();
    read.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK)), component, view);

    final ShowWriteAccessUsagesAction write = new ShowWriteAccessUsagesAction();
    write.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK)), component, view);
    return new AnAction[] {read, write};
  }

  private static final class ReadWriteState {
    private boolean myShowReadAccess = true;
    private boolean myShowWriteAccess = true;

    public boolean isShowReadAccess() {
      return myShowReadAccess;
    }

    public void setShowReadAccess(final boolean showReadAccess) {
      myShowReadAccess = showReadAccess;
      if (!showReadAccess) {
        myShowWriteAccess = true;
      }
    }

    public boolean isShowWriteAccess() {
      return myShowWriteAccess;
    }

    public void setShowWriteAccess(final boolean showWriteAccess) {
      myShowWriteAccess = showWriteAccess;
      if (!showWriteAccess) {
        myShowReadAccess = true;
      }
    }
  }

  private class ShowReadAccessUsagesAction extends ToggleAction implements DumbAware {
    private ShowReadAccessUsagesAction() {
      super(UsageViewBundle.message("action.show.read.access"), null, AllIcons.Actions.ShowReadAccess);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return myReadWriteState.isShowReadAccess();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myReadWriteState.setShowReadAccess(state);
      Project project = e.getData(Project.KEY);
      if (project == null) return;
      project.getMessageBus().syncPublisher(UsageFilteringRuleListener.class).rulesChanged();
    }
  }

  private class ShowWriteAccessUsagesAction extends ToggleAction implements DumbAware {
    private ShowWriteAccessUsagesAction() {
      super(UsageViewBundle.message("action.show.write.access"), null, AllIcons.Actions.ShowWriteAccess);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return myReadWriteState.isShowWriteAccess();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myReadWriteState.setShowWriteAccess(state);
      Project project = e.getData(Project.KEY);
      if (project == null) return;
      project.getMessageBus().syncPublisher(UsageFilteringRuleListener.class).rulesChanged();
    }
  }
}
