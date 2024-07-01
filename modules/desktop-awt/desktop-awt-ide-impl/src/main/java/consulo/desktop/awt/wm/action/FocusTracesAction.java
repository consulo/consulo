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
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

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
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    final IdeFocusManager manager = IdeFocusManager.getGlobalInstance();
    if (!(manager instanceof FocusManagerImpl)) return;
    final FocusManagerImpl focusManager = (FocusManagerImpl)manager;

    myActive = !myActive;
    if (myActive) {
      myFocusTracker = event -> {
        if (event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_GAINED) {
          focusManager.getRequests().add(new FocusRequestInfo(((FocusEvent)event).getComponent(), new Throwable(), false));
        }
      };
      Toolkit.getDefaultToolkit().addAWTEventListener(myFocusTracker, AWTEvent.FOCUS_EVENT_MASK);
    }

    if (!myActive) {
      final List<FocusRequestInfo> requests = focusManager.getRequests();
      new FocusTracesDialog(project, new ArrayList<>(requests)).show();
      Toolkit.getDefaultToolkit().removeAWTEventListener(myFocusTracker);
      myFocusTracker = null;
      requests.clear();
    }
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    presentation.setText(myActive ? "Stop Focus Tracing" : "Start Focus Tracing");
    presentation.setEnabled(e.getData(Project.KEY) != null);
  }
}
