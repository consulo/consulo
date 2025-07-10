/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.usages.actions;

import consulo.application.ReadAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.usage.UsageView;
import consulo.ide.impl.idea.usages.impl.UsageViewImpl;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * @author gregsh
 */
public class RerunSearchAction extends DumbAwareAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    if (e.getData(UsageView.USAGE_VIEW_KEY) instanceof UsageViewImpl usageView) {
      usageView.refreshUsages();
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    boolean enabled = e.getData(UsageView.USAGE_VIEW_KEY) instanceof UsageViewImpl usageView
        && Objects.equals(ReadAction.compute(usageView::canPerformReRun), Boolean.TRUE);
    e.getPresentation().setEnabled(enabled);
  }
}
