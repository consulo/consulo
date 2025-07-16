/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.desktop.awt.wm.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.desktop.awt.wm.FocusManagerImpl;
import consulo.desktop.awt.wm.FocusRequestInfo;
import consulo.localize.LocalizeValue;import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "FocusTracer")
public class FocusTracesAction extends AnAction implements DumbAware {
  private static boolean myActive = false;
  private AWTEventListener myFocusTracker;

  public FocusTracesAction() {
    super("Start Focus Trace");
  }

  public static boolean isActive() {
    return myActive;
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    if (!(IdeFocusManager.getGlobalInstance() instanceof FocusManagerImpl focusManager)) return;

    myActive = !myActive;
    if (myActive) {
      myFocusTracker = event -> {
        if (event instanceof FocusEvent focusEvent && event.getID() == FocusEvent.FOCUS_GAINED) {
          focusManager.getRequests().add(new FocusRequestInfo(focusEvent.getComponent(), new Throwable(), false));
        }
      };
      Toolkit.getDefaultToolkit().addAWTEventListener(myFocusTracker, AWTEvent.FOCUS_EVENT_MASK);
    }

    if (!myActive) {
      final List<FocusRequestInfo> requests = focusManager.getRequests();
      Project project = e.getRequiredData(Project.KEY);
      new FocusTracesDialog(project, new ArrayList<>(requests)).show();
      Toolkit.getDefaultToolkit().removeAWTEventListener(myFocusTracker);
      myFocusTracker = null;
      requests.clear();
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setTextValue(
        myActive
            ? LocalizeValue.localizeTODO("Stop Focus Tracing")
            : LocalizeValue.localizeTODO("Start Focus Tracing")
    );
    presentation.setEnabled(e.hasData(Project.KEY));
  }
}
