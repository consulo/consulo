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
package consulo.execution.debug.impl.internal.action;

import consulo.execution.debug.impl.internal.DebuggerSupport;
import consulo.execution.debug.impl.internal.action.handler.DebuggerActionHandler;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class XDebuggerActionBase extends AnAction implements AnAction.TransparentUpdate {
  private final boolean myHideDisabledInPopup;

  protected XDebuggerActionBase() {
    this(false);
  }

  protected XDebuggerActionBase(final boolean hideDisabledInPopup) {
    myHideDisabledInPopup = hideDisabledInPopup;
  }

  @Override
  @RequiredUIAccess
  public void update(final AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    boolean hidden = isHidden(event);
    if (hidden) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
      return;
    }

    boolean enabled = isEnabled(event);
    if (myHideDisabledInPopup && ActionPlaces.isPopupPlace(event.getPlace())) {
      presentation.setVisible(enabled);
    }
    else {
      presentation.setVisible(true);
    }
    presentation.setEnabled(enabled);
  }

  protected boolean isEnabled(final AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project != null) {
      for (DebuggerSupport support : DebuggerSupport.getDebuggerSupports()) {
        if (isEnabled(project, e, support)) {
          return true;
        }
      }
    }
    return false;
  }

  @Nonnull
  protected abstract DebuggerActionHandler getHandler(@Nonnull DebuggerSupport debuggerSupport);

  private boolean isEnabled(final Project project, final AnActionEvent event, final DebuggerSupport support) {
    return getHandler(support).isEnabled(project, event);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    performWithHandler(e);
  }

  protected boolean performWithHandler(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return true;
    }

    for (DebuggerSupport support : DebuggerSupport.getDebuggerSupports()) {
      if (isEnabled(project, e, support)) {
        perform(project, e, support);
        return true;
      }
    }
    return false;
  }

  private void perform(final Project project, final AnActionEvent e, final DebuggerSupport support) {
    getHandler(support).perform(project, e);
  }

  protected boolean isHidden(AnActionEvent event) {
    final Project project = event.getData(Project.KEY);
    if (project != null) {
      for (DebuggerSupport support : DebuggerSupport.getDebuggerSupports()) {
        if (!getHandler(support).isHidden(project, event)) {
          return false;
        }
      }
    }
    return true;
  }
}
